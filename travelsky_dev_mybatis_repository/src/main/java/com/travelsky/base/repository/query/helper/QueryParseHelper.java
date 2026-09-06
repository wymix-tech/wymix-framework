package com.travelsky.base.repository.query.helper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.common.query.OperateSymbol;
import com.travelsky.base.common.query.SortVO;
import com.travelsky.base.repository.query.FilterSymbolConst;
import com.travelsky.base.utils.StringUtils;
import com.travelsky.base.utils.json.JsonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

public class QueryParseHelper {

    /**
     * 列名风格策略：根据 {@code COLUMN_NAME_RULE} 系统属性决定是否做驼峰转下划线。
     * <p>缓存只读快照，配置变更时通过 {@link #refresh()} 重建，避免静态可变状态带来的可见性问题。
     */
    private static volatile ColumnNameStyleStrategy strategy = ColumnNameStyleStrategy.fromSystemProperty();

    /**
     * 兼容旧版本预留的入口：刷新列名风格策略（重新读取系统属性）。
     * <p>仅当运行期通过 {@link System#setProperty(String, String)} 修改 {@code COLUMN_NAME_RULE} 后调用。
     */
    public static void refresh() {
        strategy = ColumnNameStyleStrategy.fromSystemProperty();
    }

    static void setStrategy(ColumnNameStyleStrategy newStrategy) {
        strategy = newStrategy == null ? ColumnNameStyleStrategy.fromSystemProperty() : newStrategy;
    }

    static boolean isCamelToUnderline() {
        return strategy.camelToUnderline();
    }

    public static <T> QueryWrapper<T> parseToQueryWrapper(QueryWrapper<T> wrapper, DevQueryVO query, Class<T> clazz) {
        if (StringUtils.isNotEmpty(query.getFilter())) {
            parse(wrapper, query.getFilter(), clazz);
        }
        String sort = query.getSorts();
        if (StringUtils.isNotEmpty(sort)) {
            List<SortVO> sortVOs = JsonUtils.jsonToEntity(List.class, SortVO.class, sort);
            for (SortVO sortVO : sortVOs) {
                if (sortVO.getDirection() == SortVO.Type.ASC) {
                    wrapper.orderByAsc(Arrays.stream(sortVO.getColumns()).map(e->toColumnName(e,clazz)).toList());
                } else {
                    wrapper.orderByDesc(Arrays.stream(sortVO.getColumns()).map(e->toColumnName(e,clazz)).toList());
                }
            }
        }
        return wrapper;
    }

    private static <T> void parse(QueryWrapper<T> wrapper, String condition, Class<T> clazz) {
        try {
            // 解析JSON为List<Object>
            List<Object> conditions = FilterStringParser.parse(condition);
            // 创建QueryWrapper并解析条件
            parseConditions(conditions, wrapper, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse input: " + condition, e);
        }
    }

    private static <T> void parseConditions(List<Object> conditions, QueryWrapper<T> wrapper, Class<T> clazz) {
        Iterator<Object> iterator = conditions.iterator();
        while (iterator.hasNext()) {
            Object element = iterator.next();
            if (element instanceof List) {
                List<Object> subList = (List<Object>) element;
                if (isCondition(subList)) {
                    addCondition(wrapper, subList, clazz);
                } else {
                    wrapper.and(w -> parseConditions(subList, w, clazz));
                }
            } else if (element instanceof String) {
                String operator = (String) element;
                if (iterator.hasNext()) {
                    Object next = iterator.next();
                    if (next instanceof List) {
                        List<Object> rightOperand = (List<Object>) next;
                        if ("AND".equalsIgnoreCase(operator)) {
                            wrapper.and(w -> parseConditions(isCondition(rightOperand) ? Collections.singletonList(rightOperand) : rightOperand, w, clazz));
                        } else if ("OR".equalsIgnoreCase(operator)) {
                            wrapper.or(w -> parseConditions(isCondition(rightOperand) ? Collections.singletonList(rightOperand) : rightOperand, w, clazz));
                        } else {
                            if (rightOperand.stream().noneMatch(e -> e instanceof List)) {
                                if (isCondition(rightOperand)) {
                                    addCondition(wrapper, rightOperand, clazz);
                                } else {
                                    wrapper.and(w -> parseConditions(isCondition(rightOperand) ? Collections.singletonList(rightOperand) : rightOperand, w, clazz));
                                }
                            } else {
                                wrapper.or(w -> parseConditions(isCondition(rightOperand) ? Collections.singletonList(rightOperand) : rightOperand, w, clazz));
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isCondition(List<Object> list) {
        return list.size() >= 3 && list.get(0) instanceof String && list.get(1) instanceof String;
    }

    private static <T> void addCondition(QueryWrapper<T> wrapper, List<Object> condition, Class<T> clazz) {
        String field = (String) condition.get(0);
        OperateSymbol operator = OperateSymbol.of((String) condition.get(1));
        List<Object> values = condition.subList(2, condition.size());

        switch (operator) {
            case EQ:
                wrapper.eq(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case NE:
                wrapper.ne(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case GE:
                wrapper.ge(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case LE:
                wrapper.le(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case GT:
                wrapper.gt(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case LT:
                wrapper.lt(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case CONTAINS:
                wrapper.like(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case LIKE_LEFT:
                wrapper.likeLeft(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case LIKE_RIGHT:
                wrapper.likeRight(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()));
                break;
            case BETWEEN:
                if (values.size() == 2) {
                    wrapper.between(toColumnName(field, clazz), FieldHelper.translate(clazz, field, values.get(0).toString()), FieldHelper.translate(clazz, field, values.get(1).toString()));
                }
                break;
            case IN:
                wrapper.in(toColumnName(field, clazz), values.stream().map(v -> FieldHelper.translate(clazz, field, v.toString())).toList());
                break;
            case NOT_IN:
                wrapper.notIn(toColumnName(field, clazz), values.stream().map(v -> FieldHelper.translate(clazz, field, v.toString())).toList());
                break;
            case NULL:
                wrapper.isNull(toColumnName(field, clazz));
                break;
            case NOT_NULL:
                wrapper.isNotNull(toColumnName(field, clazz));
                break;
            default:
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    private static final <T> String toColumnName(String field, Class<T> clazz) {
        return FieldHelper.addConditionAlias(clazz, field, formatToColumnName(field));
    }
    public static String formatToColumnName(String field) {
        String tableAlias = null;
        if (field.contains(".")){
            String[] temp = field.split("\\.");
            tableAlias=temp[0];
            field = temp[1];
        }
        return (StringUtils.isNotEmpty(tableAlias)?tableAlias+".":"")+(isCamelToUnderline() ? StringUtils.camelToUnderline(field) : field);
    }

    /**
     * 列名风格策略（任务4：原静态可变状态 CAMEL_TO_UNDERLINE 改为不可变策略对象）。
     */
    public static final class ColumnNameStyleStrategy {

        private final boolean camelToUnderline;

        private ColumnNameStyleStrategy(boolean camelToUnderline) {
            this.camelToUnderline = camelToUnderline;
        }

        public boolean camelToUnderline() {
            return camelToUnderline;
        }

        public static ColumnNameStyleStrategy camelToUnderline(boolean camelToUnderline) {
            return new ColumnNameStyleStrategy(camelToUnderline);
        }

        public static ColumnNameStyleStrategy fromSystemProperty() {
            return new ColumnNameStyleStrategy("CAMEL_TO_UNDERLINE".equals(System.getProperty("COLUMN_NAME_RULE", "CAMEL_TO_UNDERLINE")));
        }
    }

    public static class FilterStringParser {

        // 入口方法：将字符串解析为结构化条件
        public static List<Object> parse(String filter) {
            filter = filter.trim();
            if (!filter.startsWith(FilterSymbolConst.LEFT_BUCKET_STR) || !filter.endsWith(FilterSymbolConst.RIGHT_BUCKET_STR)) {
                throw new IllegalArgumentException("Invalid filter format: must start with '[' and end with FilterSymbolConst.RIGHT_BUCKET");
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
                } else if (ch == FilterSymbolConst.COMMA || ch == ' ') {
                    // 分隔符，跳过
                    index++;
                } else if (ch == FilterSymbolConst.RIGHT_BUCKET) {
                    // 列表结束
                    break;
                } else {
                    // 解析字符串（条件或逻辑符）
                    int nextComma = filter.indexOf(FilterSymbolConst.COMMA, index);
                    int nextBracket = filter.indexOf(']', index);
                    int next = Math.min(nextComma == -1 ? Integer.MAX_VALUE : nextComma, nextBracket == -1 ? Integer.MAX_VALUE : nextBracket);
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
