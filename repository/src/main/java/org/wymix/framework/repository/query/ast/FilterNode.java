package org.wymix.framework.repository.query.ast;

/**
 * filter DSL 统一抽象语法树节点。
 * <p>由 {@link org.wymix.framework.repository.query.FilterParser} 从 filter 字符串解析产出，
 * JPA / MyBatis 翻译层各自将 AST 翻译为 Specification / QueryWrapper，词法与语法逻辑只维护一份。
 */
public sealed interface FilterNode permits ConditionNode, LogicalNode, GroupNode {
}
