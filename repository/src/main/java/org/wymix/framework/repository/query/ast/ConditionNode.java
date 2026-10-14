package org.wymix.framework.repository.query.ast;

import org.wymix.framework.common.query.OperateSymbol;

import java.util.List;

/**
 * 叶子条件节点，如 {@code [field,OP,value...]}。
 *
 * @param field    字段名（可含表别名前缀，如 {@code u.userName}）
 * @param operator 操作符
 * @param values   操作数值列表（IN 多值 / BETWEEN 双值，其余单值）
 */
public record ConditionNode(String field, OperateSymbol operator, List<Object> values) implements FilterNode {
    public ConditionNode {
        if (field == null || field.isBlank() || operator == null || values == null) {
            throw new IllegalArgumentException("Filter field and operator are required");
        }
        int expected = switch (operator) {
            case NULL, NOT_NULL -> 0;
            case BETWEEN -> 2;
            case IN, NOT_IN -> -1;
            default -> 1;
        };
        if ((expected >= 0 && values.size() != expected) || (expected < 0 && values.isEmpty())) {
            throw new IllegalArgumentException("Incorrect operand count for " + operator);
        }
        values = List.copyOf(values);
    }
}
