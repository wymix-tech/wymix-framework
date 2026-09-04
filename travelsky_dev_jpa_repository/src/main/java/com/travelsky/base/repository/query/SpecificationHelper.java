package com.travelsky.base.repository.query;

import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.common.query.OperateSymbol;
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
import java.util.Stack;

public class SpecificationHelper {

    private SpecificationHelper() {

    }

    public static <T> Predicate parse(DevQueryVO devQueryVO, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
        if (StringUtils.isBlank(devQueryVO.getFilter())) {
            return null;
        }

        return new SpecificationParser<T>().parse(devQueryVO.getFilter(), root, query, criteriaBuilder);
    }


    static class SpecificationParser<T> {

        // 入口方法：解析过滤条件
        public Predicate parse(String filter, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            List<Object> structuredFilter = FilterStringParser.parse(filter);
            return buildSpecification(structuredFilter, root, query, criteriaBuilder);
        }

        // 递归解析条件或逻辑组合
        private Predicate buildSpecification(Object node, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            if (node instanceof List) {
                List<?> nodeList = (List<?>) node;
                if (isLogicalExpression(nodeList)) {
                    // 处理逻辑组合（如 [left, "and", right]）
                    return parseLogicalExpression(nodeList, root, query, criteriaBuilder);
                } else if (isCondition(nodeList)) {
                    // 处理条件（如 ["age", ">", 18]）
                    return parseCondition(nodeList, root, query, criteriaBuilder);
                } else {
                    // 默认多个条件用 AND 连接（如 [cond1, cond2] → cond1 AND cond2）
                    return combineConditionsWithAnd(nodeList, root, query, criteriaBuilder);
                }
            }
            throw new IllegalArgumentException("Invalid filter node: " + node);
        }

        // 判断是否为逻辑组合（如 [left, "and", right]）
        private boolean isLogicalExpression(List<?> nodeList) {
            return nodeList.size() == 3 && nodeList.get(1) instanceof String
                    && (OperateSymbol.AND.equalsSymbol((String) nodeList.get(1))
                    || OperateSymbol.OR.equalsSymbol(((String) nodeList.get(1))));
        }

        // 判断是否为条件（如 ["age", ">", 18]）
        private boolean isCondition(List<?> nodeList) {
            return nodeList.size() == 3 && nodeList.get(1) instanceof String
                    && !OperateSymbol.AND.equalsSymbol((String) nodeList.get(1))
                    && !OperateSymbol.OR.equalsSymbol((String) nodeList.get(1));
        }

        // 解析逻辑组合
        private Predicate parseLogicalExpression(List<?> nodeList, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            Predicate left = buildSpecification(nodeList.get(0), root, query, criteriaBuilder);
            Predicate right = buildSpecification(nodeList.get(2), root, query, criteriaBuilder);
            OperateSymbol operator = OperateSymbol.of(((String) nodeList.get(1)).toLowerCase());
            return combineSpecifications(left, right, operator, criteriaBuilder);
        }

        // 合并逻辑组合（AND/OR）
        private Predicate combineSpecifications(Predicate left, Predicate right, OperateSymbol operator, CriteriaBuilder criteriaBuilder) {

            Predicate predicate;
            if (OperateSymbol.AND == operator) {
                predicate = criteriaBuilder.and(left, right);
            } else {
                predicate = criteriaBuilder.or(left, right);
            }
            return predicate;

        }

        // 解析单个条件（如 ["name", "contains", "Alice"] → name LIKE '%Alice%'）
        private Predicate parseCondition(List<?> condition, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            String field = (String) condition.get(0);
            OperateSymbol operator = OperateSymbol.of(((String) condition.get(1)).toUpperCase());
            Object value = condition.get(2);
            Path<?> path = resolveFieldPath(root, field);
            return buildPredicate(path, operator, value, criteriaBuilder);
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

        // 构建条件谓词（支持 =, !=, contains, >, <, >=, <=, in, between）
        private Predicate buildPredicate(Path<?> path, OperateSymbol operator, Object value, CriteriaBuilder cb) {
            switch (operator) {
                case EQ:
                    return cb.equal(path, convertValue(path.getJavaType(), value));
                case NE:
                    return cb.notEqual(path, convertValue(path.getJavaType(), value));
                case CONTAINS:
                    return cb.like(path.as(String.class), "%" + value.toString() + "%");
                case LIKE_LEFT:
                    return cb.like(path.as(String.class), value.toString() + "%");
                case LIKE_RIGHT:
                    return cb.like(path.as(String.class), "%" + value.toString());
                case GT:
                    return cb.greaterThan(path.as(Comparable.class), (Comparable) value);
                case LT:
                    return cb.lessThan(path.as(Comparable.class), (Comparable) value);
                case GE:
                    return cb.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
                case LE:
                    return cb.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
                case BETWEEN:
                    return buildBetweenPredicate(path, value, cb);
                case IN:
                    return path.in(convertToCollection(value));
                case NOT_IN:
                    return cb.not(path.in(convertToCollection(value)));
                case NULL:
                    return cb.isNull(path);
                case NOT_NULL:
                    return cb.isNotNull(path);
                default:
                    throw new IllegalArgumentException("Unsupported operator: " + operator);
            }
        }

        // 将值转换为集合（用于 `in` 操作符）
        private Collection<?> convertToCollection(Object value) {
            if (value instanceof Collection) {
                return (Collection<?>) value;
            } else if (value instanceof Object[]) {
                return Arrays.asList((Object[]) value);
            } else {
                throw new IllegalArgumentException("Value for 'in' operator must be a collection or array");
            }
        }

        // 构建 `between` 条件
        private Predicate buildBetweenPredicate(Path<?> path, Object value, CriteriaBuilder cb) {
            if (!(value instanceof List) || ((List<?>) value).size() != 2) {
                throw new IllegalArgumentException("Value for 'between' operator must be a list of two elements");
            }
            List<?> range = (List<?>) value;
            Comparable lower = (Comparable) convertValue(path.getJavaType(), range.get(0));
            Comparable upper = (Comparable) convertValue(path.getJavaType(), range.get(1));
            return cb.between(path.as(Comparable.class), lower, upper);
        }

        // 类型转换（如 String → Integer/Date）
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
        private Predicate combineConditionsWithAnd(List<?> nodes, Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            Predicate combined = null;
            for (Object node : nodes) {
                Predicate spec = buildSpecification(node, root, query, criteriaBuilder);
                combined = combined == null ? spec : criteriaBuilder.and(combined, spec);
            }
            return combined;
        }
    }

    static class FilterStringParser {

        // 入口方法：将字符串解析为结构化条件
        static List<Object> parse(String filter) {
            filter = filter.trim();
            if (!filter.startsWith(FilterSymbolConst.LEFT_BUCKET_STR) || !filter.endsWith(FilterSymbolConst.RIGHT_BUCKET_STR)) {
                throw new IllegalArgumentException("Invalid filter format: must start with '[' and end with ']'");
            }
            return parseList(filter, 1, filter.length() - 1);
        }

        // 递归解析列表
        private static List<Object> parseList(String filter, int start, int end) {
            List<Object> result = new ArrayList<>();
            int index = start;
            while (index < end) {
                char ch = filter.charAt(index);
                if (ch == FilterSymbolConst.LEFT_BUCKET) {
                    // 嵌套列表
                    int closeIndex = findClosingBracket(filter, index);
                    result.add(parseList(filter, index + 1, closeIndex));
                    index = closeIndex + 1;
                } else if (ch == FilterSymbolConst.COMMA) {
                    // 分隔符，跳过
                    index++;
                } else if (ch == FilterSymbolConst.RIGHT_BUCKET) {
                    // 列表结束
                    break;
                } else {
                    // 解析字符串（条件或逻辑符）
                    int nextComma = filter.indexOf(FilterSymbolConst.COMMA, index);
                    int nextBracket = filter.indexOf(FilterSymbolConst.RIGHT_BUCKET, index);
                    int next = Math.min(nextComma == -1 ? Integer.MAX_VALUE : nextComma,
                            nextBracket == -1 ? Integer.MAX_VALUE : nextBracket);
                    if (next == Integer.MAX_VALUE) {
                        throw new IllegalArgumentException("Invalid filter format: missing ',' or ']'");
                    }
                    String token = filter.substring(index, next).trim();
                    if (!token.isEmpty()) {
                        result.add(token);
                    }
                    index = next;
                }
            }
            return result;
        }

        // 找到匹配的闭合括号
        private static int findClosingBracket(String filter, int start) {
            Stack<Character> stack = new Stack<>();
            stack.push(FilterSymbolConst.LEFT_BUCKET);
            for (int i = start + 1; i < filter.length(); i++) {
                char ch = filter.charAt(i);
                if (ch == FilterSymbolConst.LEFT_BUCKET) {
                    stack.push(FilterSymbolConst.LEFT_BUCKET);
                } else if (ch == FilterSymbolConst.RIGHT_BUCKET) {
                    stack.pop();
                    if (stack.isEmpty()) {
                        return i;
                    }
                }
            }
            throw new IllegalArgumentException("Invalid filter format: unmatched '['");
        }
    }

}
