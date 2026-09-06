package com.travelsky.base.repository.query.ast;

/**
 * 二元逻辑节点（AND / OR）。
 *
 * @param type  逻辑类型
 * @param left  左子节点
 * @param right 右子节点
 */
public record LogicalNode(Type type, FilterNode left, FilterNode right) implements FilterNode {

    public enum Type { AND, OR }
}
