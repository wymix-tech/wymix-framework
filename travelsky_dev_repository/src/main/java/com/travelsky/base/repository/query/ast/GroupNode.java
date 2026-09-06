package com.travelsky.base.repository.query.ast;

import java.util.List;

/**
 * 分组节点：多个子节点按原顺序以 AND 语义合并（与既有行为一致，如 {@code [cond1, cond2]}）。
 */
public record GroupNode(List<FilterNode> children) implements FilterNode {
}
