package com.travelsky.base.repository.query;

import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.common.query.OperateSymbol;
import com.travelsky.base.core.context.ApplicationContextHolder;
import com.travelsky.base.repository.datapermission.context.DataPermissionContextHolder;
import com.travelsky.base.repository.datapermission.enums.InjectType;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;
import com.travelsky.base.repository.query.ast.ConditionNode;
import com.travelsky.base.repository.query.ast.FilterNode;
import com.travelsky.base.repository.query.ast.GroupNode;
import com.travelsky.base.repository.query.ast.LogicalNode;
import com.travelsky.base.utils.StringUtils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * JPA 查询构造器：将统一 filter DSL 的 {@link FilterNode} AST 翻译为 {@link Predicate}。
 * <p>filter 词法/语法解析已上提至 travelsky_dev_repository（{@link FilterParser}），
 * 本类仅负责 AST -> Specification 的翻译，新增操作符只需在 {@link #buildPredicate} 增加分支。
 *
 * <p>数据权限：进入 {@code @DataPermission} 方法后，{@link #applyDataPermission} 会读取
 * {@link DataPermissionContextHolder} 中声明的注入器，把 {@code WHERE_ADDITIONAL} 型
 * {@code injectSql()} 翻译为 {@link Predicate} 追加到查询条件上，与 MyBatis 侧行为对齐。
 */
public class SpecificationHelper {

    private SpecificationHelper() {

    }

    public static <T> Predicate parse(DevQueryVO devQueryVO, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (StringUtils.isBlank(devQueryVO.getFilter())) {
            return applyDataPermission(root, criteriaBuilder);
        }

        Predicate filterPredicate = new SpecificationTranslator<T>().translate(devQueryVO.getFilter(), root, query, criteriaBuilder);
        Predicate permissionPredicate = applyDataPermission(root, criteriaBuilder);
        if (filterPredicate == null) {
            return permissionPredicate;
        }
        if (permissionPredicate == null) {
            return filterPredicate;
        }
        return criteriaBuilder.and(filterPredicate, permissionPredicate);
    }

    /**
     * 将数据权限上下文中的 {@code WHERE_ADDITIONAL} 注入器翻译为 {@link Predicate}。
     *
     * <p>injectSql 语法约定（与 MyBatis 侧 {@code PermissionSqlInjector} 保持一致）：
     * <ul>
     *   <li>{@code param = :value} —— 命名参数，值取自 {@link PermissionSqlInjector#additionalParameters()}；</li>
     *   <li>{@code COLUMN = 'CONSTANT'} / {@code COLUMN = 123} —— 字面量常量；</li>
     *   <li>{@code A->B} 形式为 {@code REPLACE_TABLE_NAME} 语义，JPA 查询基于实体映射，不适用，跳过。</li>
     * </ul>
     * 未进入 {@code @DataPermission} 上下文或无可用的 WHERE 注入器时返回 {@code null}，即不追加任何条件。
     */
    public static <T> Predicate applyDataPermission(Root<T> root, CriteriaBuilder criteriaBuilder) {
        if (!DataPermissionContextHolder.exists()) {
            return null;
        }
        DataPermissionContextHolder.DataPermissionContext context = DataPermissionContextHolder.getContext();
        if (context == null || context.getInjectors() == null) {
            return null;
        }

        Predicate combined = null;
        for (Class<? extends PermissionSqlInjector> injectorClass : context.getInjectors()) {
            PermissionSqlInjector injector = ApplicationContextHolder.getBean(injectorClass);
            // REPLACE_TABLE_NAME 依赖 SQL 文本改写，JPA 基于实体/属性映射，不适用
            if (injector.type() != InjectType.WHERE_ADDITIONAL) {
                continue;
            }
            String injectSql = injector.injectSql();
            if (StringUtils.isBlank(injectSql)) {
                continue;
            }
            Predicate predicate = translateInjectSql(injectSql.trim(), injector, root, criteriaBuilder);
            combined = combined == null ? predicate : criteriaBuilder.and(combined, predicate);
        }
        return combined;
    }

    /**
     * 把单条 injectSql（{@code COLUMN = :param} 或 {@code COLUMN = 常量}）翻译为 {@link Predicate}。
     * 仅支持 {@code =} 的单条件；复杂条件请拆分为多个注入器（多个注入器之间为 AND 语义）。
     */
    private static <T> Predicate translateInjectSql(String injectSql, PermissionSqlInjector injector, Root<T> root, CriteriaBuilder criteriaBuilder) {
        int eqIndex = injectSql.indexOf('=');
        if (eqIndex <= 0 || eqIndex == injectSql.length() - 1) {
            throw new IllegalArgumentException("Unsupported JPA data permission injectSql (expect 'COLUMN = value'): " + injectSql);
        }
        String column = injectSql.substring(0, eqIndex).trim();
        String rawValue = injectSql.substring(eqIndex + 1).trim();

        Path<?> path = resolvePath(root, column);
        Object value;
        if (rawValue.startsWith(":")) {
            String paramName = rawValue.substring(1).trim();
            Map<String, Object> params = injector.additionalParameters();
            if (params == null || !params.containsKey(paramName)) {
                throw new IllegalArgumentException("Missing additional parameter for data permission injectSql: " + paramName);
            }
            value = params.get(paramName);
        } else {
            value = stripQuotes(rawValue);
        }

        if (value == null) {
            return criteriaBuilder.isNull(path);
        }
        return criteriaBuilder.equal(path.as(value.getClass()), value);
    }

    private static Path<?> resolvePath(Root<?> root, String column) {
        String property = column;
        int dot = property.indexOf('.');
        if (dot >= 0) {
            // 支持 "alias.column"（仅取列部分）；深层嵌套属性可继续用 '.'
            String prefix = property.substring(0, dot);
            if (prefix.equals(root.getJavaType().getSimpleName()) || prefix.length() == 0) {
                property = property.substring(dot + 1);
            }
        }
        String[] parts = property.split("\\.");
        Path<?> path = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        return path;
    }

    private static Object stripQuotes(String rawValue) {
        if (rawValue.length() >= 2 && ((rawValue.startsWith("'") && rawValue.endsWith("'"))
                || (rawValue.startsWith("\"") && rawValue.endsWith("\"")))) {
            return rawValue.substring(1, rawValue.length() - 1);
        }
        // 数字字面量
        try {
            return Long.valueOf(rawValue);
        } catch (NumberFormatException ignore) {
            // fall through
        }
        try {
            return Double.valueOf(rawValue);
        } catch (NumberFormatException ignore) {
            // fall through
        }
        return rawValue;
    }


    static class SpecificationTranslator<T> {

        // 入口方法：解析 filter 为统一 AST 后翻译为 Predicate
        public Predicate translate(String filter, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            FilterNode ast = FilterParser.parse(filter);
            if (ast == null) {
                return null;
            }
            return toPredicate(ast, root, query, criteriaBuilder);
        }

        // AST -> Predicate：按节点类型分派
        private Predicate toPredicate(FilterNode node, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            if (node instanceof ConditionNode condition) {
                return toPredicate(condition, root, criteriaBuilder);
            }
            if (node instanceof LogicalNode logical) {
                return toPredicate(logical, root, query, criteriaBuilder);
            }
            if (node instanceof GroupNode group) {
                return combineConditionsWithAnd(group.children(), root, query, criteriaBuilder);
            }
            throw new IllegalArgumentException("Invalid filter node: " + node);
        }

        // 解析逻辑组合（AND/OR）
        private Predicate toPredicate(LogicalNode node, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            Predicate left = toPredicate(node.left(), root, query, criteriaBuilder);
            Predicate right = toPredicate(node.right(), root, query, criteriaBuilder);
            if (node.type() == LogicalNode.Type.AND) {
                return criteriaBuilder.and(left, right);
            }
            return criteriaBuilder.or(left, right);
        }

        // 解析单个条件（如 ["name", "contains", "Alice"] -> name LIKE '%Alice%'）
        private Predicate toPredicate(ConditionNode condition, Root<T> root, CriteriaBuilder criteriaBuilder) {
            Path<?> path = resolveFieldPath(root, condition.field());
            return buildPredicate(path, condition.operator(), condition.values(), criteriaBuilder);
        }

        // 处理嵌套字段（如 user.address.city）
        private Path<?> resolveFieldPath(Root<T> root, String field) {
            String[] parts = field.split("\\.");
            Path<?> path = root.get(parts[0]);
            for (int i = 1; i < parts.length; i++) {
                path = path.get(parts[i]);
            }
            return path;
        }

        // 构建条件谓词（支持 =, !=, contains, >, <, >=, <=, in, between, is null, is not null）
        private Predicate buildPredicate(Path<?> path, OperateSymbol operator, List<Object> values, CriteriaBuilder cb) {
            switch (operator) {
                case EQ:
                    return cb.equal(path, convertValue(path.getJavaType(), values.get(0)));
                case NE:
                    return cb.notEqual(path, convertValue(path.getJavaType(), values.get(0)));
                case CONTAINS:
                    return cb.like(path.as(String.class), "%" + values.get(0).toString() + "%");
                case LIKE_LEFT:
                    return cb.like(path.as(String.class), values.get(0).toString() + "%");
                case LIKE_RIGHT:
                    return cb.like(path.as(String.class), "%" + values.get(0).toString());
                case GT:
                    return cb.greaterThan(path.as(Comparable.class), (Comparable) convertValue(path.getJavaType(), values.get(0)));
                case LT:
                    return cb.lessThan(path.as(Comparable.class), (Comparable) convertValue(path.getJavaType(), values.get(0)));
                case GE:
                    return cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) convertValue(path.getJavaType(), values.get(0)));
                case LE:
                    return cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) convertValue(path.getJavaType(), values.get(0)));
                case BETWEEN:
                    return buildBetweenPredicate(path, values, cb);
                case IN:
                    return path.in(convertToCollection(values));
                case NOT_IN:
                    return cb.not(path.in(convertToCollection(values)));
                case NULL:
                    return cb.isNull(path);
                case NOT_NULL:
                    return cb.isNotNull(path);
                default:
                    throw new IllegalArgumentException("Unsupported operator: " + operator);
            }
        }

        // 将值转换为集合（用于 `in` 操作符）
        private Collection<?> convertToCollection(List<Object> values) {
            return values;
        }

        // 构建 `between` 条件
        private Predicate buildBetweenPredicate(Path<?> path, List<Object> values, CriteriaBuilder cb) {
            if (values.size() != 2) {
                throw new IllegalArgumentException("Value for 'between' operator must be a list of two elements");
            }
            Comparable lower = (Comparable) convertValue(path.getJavaType(), values.get(0));
            Comparable upper = (Comparable) convertValue(path.getJavaType(), values.get(1));
            return cb.between(path.as(Comparable.class), lower, upper);
        }

        // 类型转换（如 String -> Integer/Date）
        private Object convertValue(Class<?> targetType, Object value) {
            if (value == null) {
                return null;
            }
            if (targetType.isAssignableFrom(value.getClass())) {
                return value;
            }
            if (targetType == Integer.class) {
                return Integer.parseInt(value.toString());
            } else if (targetType == Long.class) {
                return Long.parseLong(value.toString());
            } else if (targetType == LocalDate.class) {
                return LocalDate.parse(value.toString());
            } else if (targetType == LocalDateTime.class) {
                return LocalDateTime.parse(value.toString());
            } else if (targetType == String.class) {
                return value.toString();
            }
            throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
        }

        // 默认合并多个条件为 AND
        private Predicate combineConditionsWithAnd(List<FilterNode> nodes, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            Predicate combined = null;
            for (FilterNode node : nodes) {
                Predicate spec = toPredicate(node, root, query, criteriaBuilder);
                combined = combined == null ? spec : criteriaBuilder.and(combined, spec);
            }
            return combined;
        }
    }
}
