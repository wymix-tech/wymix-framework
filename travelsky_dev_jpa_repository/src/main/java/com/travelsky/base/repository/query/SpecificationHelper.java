package com.travelsky.base.repository.query;

import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.common.query.OperateSymbol;
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

/**
 * JPA 查询构造器：将统一 filter DSL 的 {@link FilterNode} AST 翻译为 {@link Predicate}。
 * <p>filter 词法/语法解析已上提至 travelsky_dev_repository（{@link FilterParser}），
 * 本类仅负责 AST -> Specification 的翻译，新增操作符只需在 {@link #buildPredicate} 增加分支。
 */
public class SpecificationHelper {

    private SpecificationHelper() {

    }

    public static <T> Predicate parse(DevQueryVO devQueryVO, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (StringUtils.isBlank(devQueryVO.getFilter())) {
            return null;
        }

        return new SpecificationTranslator<T>().translate(devQueryVO.getFilter(), root, query, criteriaBuilder);
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
