package com.travelsky.base.repository.query.ast;

import com.travelsky.base.common.query.OperateSymbol;

import java.util.List;

/**
 * 叶子条件节点，如 {@code [field,OP,value...]}。
 *
 * @param field    字段名（可含表别名前缀，如 {@code u.userName}）
 * @param operator 操作符
 * @param values   操作数值列表（IN 多值 / BETWEEN 双值，其余单值）
 */
public record ConditionNode(String field, OperateSymbol operator, List<Object> values) implements FilterNode {
}
