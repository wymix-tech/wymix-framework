package org.wymix.framework.repository.query;

import org.wymix.framework.common.query.DevQueryVO;
import org.wymix.framework.common.query.OperateSymbol;
import org.wymix.framework.repository.datapermission.context.DataPermissionContextHolder;
import org.wymix.framework.repository.datapermission.enums.InjectType;
import org.wymix.framework.repository.datapermission.inject.PermissionSqlInjector;
import org.wymix.framework.repository.datapermission.support.DataPermissionSupport;
import org.wymix.framework.repository.query.ast.ConditionNode;
import org.wymix.framework.repository.query.ast.FilterNode;
import org.wymix.framework.repository.query.ast.GroupNode;
import org.wymix.framework.repository.query.ast.LogicalNode;
import org.wymix.framework.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.Column;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * JPA 查询构造器：将统一 filter DSL 的 {@link FilterNode} AST 翻译为 {@link Predicate}。
 * <p>filter 词法/语法解析已上提至 repository（{@link FilterParser}），
 * 本类仅负责 AST -> Specification 的翻译，新增操作符只需在 {@link #buildPredicate} 增加分支。
 *
 * <p>数据权限：进入 {@code @DataPermission} 方法后，{@link #applyDataPermission} 会读取
 * {@link DataPermissionContextHolder} 中声明的注入器，把 {@code WHERE_ADDITIONAL} 型
 * {@code injectSql()} 翻译为 {@link Predicate} 追加到查询条件上，与 MyBatis 侧行为对齐。
 */
@Deprecated(since = "3.0", forRemoval = false)
public class SpecificationHelper {

    private static final Logger log = LoggerFactory.getLogger(SpecificationHelper.class);

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
     * <p>injectSql 语法约定（与 MyBatis 侧 {@code PermissionSqlInjector} 保持一致，解析见
     * {@link DataPermissionSupport#parseWhereTerms}）：
     * <ul>
     *   <li>{@code param = :value} —— 命名参数，值取自 {@link PermissionSqlInjector#additionalParameters()}；</li>
     *   <li>{@code COLUMN = 'CONSTANT'} / {@code COLUMN = 123} —— 字面量常量；</li>
     *   <li>多条件以 {@code AND} 连接：{@code DEPT_ID = :deptId AND STATUS = 1}；</li>
     *   <li>{@code A->B} 形式为 {@code REPLACE_TABLE_NAME} 语义，JPA 查询基于实体映射，不适用，跳过。</li>
     * </ul>
     *
     * <p><b>字段名取值</b>：{@code injectSql} 写数据库列名（{@code CREATED_BY}）或实体属性名（{@code createdBy}）
     * 均可，{@link #resolvePath} 会先按属性名解析，失败再按 {@code @Column(name = ...)} 反查属性名；
     * 两者都对不上时<b>快速失败</b>——数据权限漏条件等于返回全量数据，风险高于报错。
     *
     * <p>未进入 {@code @DataPermission} 上下文、白名单不匹配或无可用的 WHERE 注入器时返回 {@code null}，
     * 即不追加任何条件。
     */
    public static <T> Predicate applyDataPermission(Root<T> root, CriteriaBuilder criteriaBuilder) {
        if (!DataPermissionContextHolder.exists()) {
            return null;
        }
        DataPermissionContextHolder.DataPermissionContext context = DataPermissionContextHolder.getContext();
        if (context == null || context.getInjectors() == null || context.getInjectors().isEmpty()) {
            return null;
        }
        // JPA 无 MappedStatement id 概念：显式配置了 statementIds 白名单（不含 ALL）时跳过并告警
        if (!DataPermissionSupport.matchesAllStatements(context.getStatementIds())) {
            return null;
        }

        Predicate combined = null;
        for (Class<? extends PermissionSqlInjector> injectorClass : context.getInjectors()) {
            PermissionSqlInjector injector = DataPermissionSupport.resolveInjector(injectorClass);
            // REPLACE_TABLE_NAME 依赖 SQL 文本改写，JPA 基于实体/属性映射，不适用
            if (injector.type() != InjectType.WHERE_ADDITIONAL) {
                log.debug("数据权限注入器 [{}] 为 REPLACE_TABLE_NAME 类型，JPA 侧跳过", injectorClass.getName());
                continue;
            }
            combined = combine(criteriaBuilder, combined, translateInjectSql(injector, root, criteriaBuilder));
        }
        return combined;
    }

    private static Predicate combine(CriteriaBuilder criteriaBuilder, Predicate combined, Predicate predicate) {
        if (predicate == null) {
            return combined;
        }
        return combined == null ? predicate : criteriaBuilder.and(combined, predicate);
    }

    /**
     * 把注入器的 injectSql（支持 {@code AND} 连接的多个 {@code =} 条件）翻译为 {@link Predicate}。
     */
    private static <T> Predicate translateInjectSql(PermissionSqlInjector injector, Root<T> root, CriteriaBuilder criteriaBuilder) {
        Predicate combined = null;
        for (DataPermissionSupport.DataPermissionTerm term : DataPermissionSupport.parseWhereTerms(injector.injectSql(), injector)) {
            Path<?> path = resolvePath(root, term.getProperty());
            Object value = term.getValue();
            if (value == null) {
                combined = combine(criteriaBuilder, combined, criteriaBuilder.isNull(path));
                continue;
            }
            combined = combine(criteriaBuilder, combined, criteriaBuilder.equal(path, value));
        }
        return combined;
    }

    /**
     * 把注入器声明的字段解析为实体属性路径。
     *
     * <p>解析顺序（写 {@code injectSql} 时列名 / 属性名 / 带表别名前缀都能兼容）：
     * <ol>
     *   <li>按原样作为属性名（含 {@code a.b.c} 嵌套路径）解析；</li>
     *   <li>逐段去掉前缀（兼容 {@code t.CREATED_BY} 这类带表别名的写法）；</li>
     *   <li>按 {@code @Column(name = ...)} 或「下划线列名 → 驼峰属性名」把列名反查为属性名。</li>
     * </ol>
     * 全部失败时<b>快速失败</b>：数据权限字段写错若静默跳过，等同于返回全量数据。
     */
    private static Path<?> resolvePath(Root<?> root, String column) {
        String property = unquote(column);
        if (StringUtils.isBlank(property)) {
            throw new IllegalArgumentException("JPA 数据权限 injectSql 缺少字段名: " + column);
        }
        String[] parts = property.split("\\.");
        for (int i = 0; i < parts.length; i++) {
            String candidate = String.join(".", java.util.Arrays.copyOfRange(parts, i, parts.length));
            try {
                return buildPath(root, candidate);
            } catch (IllegalArgumentException ignore) {
                // 可能是列名或带表别名，继续尝试后续候选
            }
            String mapped = toPropertyName(root, candidate);
            if (mapped != null) {
                return buildPath(root, mapped);
            }
        }
        throw new IllegalArgumentException("JPA 数据权限 injectSql 中的字段 [" + property
                + "] 既不是实体属性名，也无法由 @Column / 下划线命名策略映射到属性；请写实体属性名（如 createdBy），实体: "
                + root.getJavaType().getName());
    }

    private static Path<?> buildPath(Root<?> root, String property) {
        String[] parts = property.split("\\.");
        Path<?> path = root.get(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            path = path.get(parts[i]);
        }
        return path;
    }

    /**
     * 把（可能是数据库列名的）路径逐段映射为属性名：优先 {@code @Column(name = ...)}，其次下划线转驼峰。
     *
     * @return 映射后的属性路径；任一段映射不出来时返回 {@code null}
     */
    private static String toPropertyName(Root<?> root, String path) {
        String[] parts = path.split("\\.");
        StringBuilder mapped = new StringBuilder();
        for (String part : parts) {
            String property = mapSegment(root, part);
            if (property == null) {
                return null;
            }
            if (mapped.length() > 0) {
                mapped.append('.');
            }
            mapped.append(property);
        }
        return mapped.toString();
    }

    private static String mapSegment(Root<?> root, String segment) {
        String normalized = segment.toUpperCase(Locale.ROOT);
        Class<?> entityClass = root.getJavaType();
        while (entityClass != null && entityClass != Object.class) {
            for (Field field : entityClass.getDeclaredFields()) {
                Column columnAnnotation = field.getAnnotation(Column.class);
                if (columnAnnotation != null && !columnAnnotation.name().isEmpty()
                        && normalized.equals(columnAnnotation.name().toUpperCase(Locale.ROOT))) {
                    return field.getName();
                }
            }
            entityClass = entityClass.getSuperclass();
        }
        // 兜底：下划线列名 -> 驼峰属性名（Hibernate 默认命名策略），如 CREATED_BY -> createdBy
        String camel = toCamelCase(normalized);
        try {
            buildPath(root, camel);
            return camel;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String toCamelCase(String upperSnake) {
        StringBuilder builder = new StringBuilder();
        boolean upperNext = false;
        for (char c : upperSnake.toCharArray()) {
            if (c == '_') {
                upperNext = builder.length() > 0;
                continue;
            }
            if (upperNext) {
                builder.append(Character.toUpperCase(c));
                upperNext = false;
            } else {
                builder.append(Character.toLowerCase(c));
            }
        }
        return builder.toString();
    }

    private static String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && ((trimmed.startsWith("`") && trimmed.endsWith("`"))
                || (trimmed.startsWith("\"") && trimmed.endsWith("\""))
                || (trimmed.startsWith("'") && trimmed.endsWith("'")))) {
            return trimmed.substring(1, trimmed.length() - 1).trim();
        }
        return trimmed;
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
                    return cb.like(path.as(String.class), "%" + values.get(0).toString());
                case LIKE_RIGHT:
                    return cb.like(path.as(String.class), values.get(0).toString() + "%");
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
