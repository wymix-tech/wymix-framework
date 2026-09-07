package com.travelsky.base.repository.query;

import com.travelsky.base.common.query.OperateSymbol;
import com.travelsky.base.repository.query.ast.ConditionNode;
import com.travelsky.base.repository.query.ast.FilterNode;
import com.travelsky.base.repository.query.ast.GroupNode;
import com.travelsky.base.repository.query.ast.LogicalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * filter DSL 统一解析器（自 travelsky_dev_repository 抽象层上提）。
 * <p>将 {@code [[field,OP,value],and,[...]]} 形式的 filter 字符串解析为 {@link FilterNode} AST。
 * 词法解析与语法归一（AND/OR 逻辑、多条件默认 AND）全部收敛在此处，
 * JPA / MyBatis 模块只消费 AST，不再各自维护解析逻辑。
 *
 * <p>语义约定（兼容既有两套实现的行为并归一差异）：
 * <ul>
 *   <li>逻辑符 AND/OR 大小写不敏感（兼容 mybatis 侧 equalsIgnoreCase 行为）；</li>
 *   <li>同级混合 AND/OR 时 AND 优先（{@code A or B and C -> A or (B and C)}）；</li>
 *   <li>多个无条件连接的条件按 AND 合并（兼容 jpa 侧默认 AND 行为）；</li>
 *   <li>条件值归一：{@code [f,in,[a,b]]} 与 {@code [f,in,a,b]} 均解析为值列表 {@code [a,b]}，
 *       消除 jpa（嵌套列表）/ mybatis（扁平值）写法不一致。</li>
 * </ul>
 */
public final class FilterParser {

    private FilterParser() {
    }

    /**
     * 解析 filter 字符串为 AST。
     *
     * @param filter 形如 {@code [[name,=,john],or,[[age,>=,18]]]} 的过滤表达式
     * @return AST 根节点；空条件（如 {@code [[]]}）返回 {@code null}
     */
    public static FilterNode parse(String filter) {
        List<Object> structured = FilterStringParser.parse(filter);
        if (structured.isEmpty()) {
            return null;
        }
        return build(structured);
    }

    /**
     * 将 FilterStringParser 产出的结构化列表归一为 AST。
     */
    static FilterNode build(List<Object> nodes) {
        List<FilterNode> terms = new ArrayList<>();
        List<LogicalNode.Type> ops = new ArrayList<>();
        for (Object node : nodes) {
            if (node instanceof String token) {
                String operator = token.trim().toUpperCase();
                if (LogicalNode.Type.AND.name().equals(operator) || LogicalNode.Type.OR.name().equals(operator)) {
                    if (terms.size() <= ops.size()) {
                        throw new IllegalArgumentException("Logical operator '" + token + "' has no left operand");
                    }
                    ops.add(typeOf(operator));
                } else {
                    throw new IllegalArgumentException("Unexpected token in filter: " + token);
                }
                continue;
            }
            FilterNode child = toNode(node);
            if (child != null) {
                terms.add(child);
            }
        }
        if (terms.isEmpty()) {
            return null;
        }
        if (terms.size() != ops.size() + 1) {
            if (terms.size() > ops.size() + 1) {
                // 多个无条件连接的条件：按 AND 分组（如 [cond1, cond2, ...]）
                return new GroupNode(terms);
            }
            throw new IllegalArgumentException("Logical operator has no right operand");
        }
        // 第一趟：折叠 AND
        List<FilterNode> orTerms = new ArrayList<>();
        List<LogicalNode.Type> orOps = new ArrayList<>();
        orTerms.add(terms.get(0));
        for (int i = 0; i < ops.size(); i++) {
            FilterNode right = terms.get(i + 1);
            if (ops.get(i) == LogicalNode.Type.AND) {
                orTerms.set(orTerms.size() - 1,
                        new LogicalNode(LogicalNode.Type.AND, orTerms.get(orTerms.size() - 1), right));
            } else {
                orTerms.add(right);
                orOps.add(LogicalNode.Type.OR);
            }
        }
        // 第二趟：左结合折叠 OR
        FilterNode result = orTerms.get(0);
        for (int i = 0; i < orOps.size(); i++) {
            result = new LogicalNode(orOps.get(i), result, orTerms.get(i + 1));
        }
        return result;
    }

    private static LogicalNode.Type typeOf(String operator) {
        return LogicalNode.Type.AND.name().equals(operator)
                ? LogicalNode.Type.AND : LogicalNode.Type.OR;
    }

    /**
     * 单个结构化节点转 AST 节点；空列表（空分组）返回 {@code null} 表示无条件。
     */
    private static FilterNode toNode(Object node) {
        if (node instanceof List<?> list) {
            if (list.isEmpty()) {
                return null;
            }
            if (isCondition(list)) {
                return toCondition(list);
            }
            return build((List<Object>) list);
        }
        throw new IllegalArgumentException("Invalid filter node: " + node);
    }

    /**
     * 结构化条件：{@code [field, OP, value...]}，与 QueryParseHelper#isCondition 语义一致。
     */
    static boolean isCondition(List<?> list) {
        return list.size() >= 3 && list.get(0) instanceof String && list.get(1) instanceof String;
    }

    private static ConditionNode toCondition(List<?> condition) {
        String field = (String) condition.get(0);
        OperateSymbol operator = OperateSymbol.of(((String) condition.get(1)).toUpperCase());
        List<Object> values = new ArrayList<>(condition.subList(2, condition.size()));
        // 值归一：唯一值为嵌套列表时展开（兼容 [f,in,[a,b]] 与 [f,in,a,b] 两种写法）
        if (values.size() == 1 && values.get(0) instanceof List<?> nested) {
            values = new ArrayList<>(nested);
        }
        return new ConditionNode(field, operator, values);
    }
}
