package org.wymix.framework.repository.query.ast;

import org.wymix.framework.common.query.OperateSymbol;
import org.wymix.framework.repository.query.FilterParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FilterParser（统一 filter DSL 解析器）单元测试：
 * 1. 词法解析 -> AST 结构正确性；
 * 2. AND/OR 优先级与左结合语义；
 * 3. 多条件默认 AND；
 * 4. 非法表达式报错。
 */
class FilterParserTest {

    @Test
    void shouldParseSimpleCondition() {
        FilterNode node = FilterParser.parse("[[name,=,john]]");
        assertInstanceOf(ConditionNode.class, node);
        ConditionNode condition = (ConditionNode) node;
        assertEquals("name", condition.field());
        assertEquals(OperateSymbol.EQ, condition.operator());
        assertEquals(List.of("john"), condition.values());
    }

    @Test
    void shouldParseLogicalExpression() {
        FilterNode node = FilterParser.parse("[[name,=,john],or,[[age,>=,18]]]");
        assertInstanceOf(LogicalNode.class, node);
        LogicalNode logical = (LogicalNode) node;
        assertEquals(LogicalNode.Type.OR, logical.type());
        assertEquals("name", ((ConditionNode) logical.left()).field());
        assertEquals("age", ((ConditionNode) logical.right()).field());
    }

    @Test
    void shouldDefaultMultipleConditionsToAnd() {
        // 与既有行为一致：[cond1, cond2] 默认 AND
        FilterNode node = FilterParser.parse("[[name,=,john],[age,>=,18]]");
        assertInstanceOf(GroupNode.class, node);
        GroupNode group = (GroupNode) node;
        assertEquals(2, group.children().size());
    }

    @Test
    void shouldGiveAndHigherPrecedenceThanOr() {
        // A or B and C -> A or (B and C)
        FilterNode node = FilterParser.parse(
                "[[a,=,1],or,[[b,=,2],and,[[c,=,3]]]]");
        assertInstanceOf(LogicalNode.class, node);
        LogicalNode logical = (LogicalNode) node;
        assertEquals(LogicalNode.Type.OR, logical.type());
        assertEquals("a", ((ConditionNode) logical.left()).field());
        assertInstanceOf(LogicalNode.class, logical.right());
        assertEquals(LogicalNode.Type.AND, ((LogicalNode) logical.right()).type());
    }

    @Test
    void shouldSupportLikeAndInOperators() {
        FilterNode node = FilterParser.parse("[[name,contains,jo]]");
        ConditionNode condition = (ConditionNode) node;
        assertEquals(OperateSymbol.CONTAINS, condition.operator());

        node = FilterParser.parse("[[status,in,[1,2,3]]]");
        condition = (ConditionNode) node;
        assertEquals(OperateSymbol.IN, condition.operator());
        assertEquals(List.of("1", "2", "3"), condition.values());
    }

    @Test
    void shouldSupportCaseInsensitiveLogicalOperators() {
        FilterNode node = FilterParser.parse("[[a,=,1],AND,[[b,=,2]]]");
        assertInstanceOf(LogicalNode.class, node);
        assertEquals(LogicalNode.Type.AND, ((LogicalNode) node).type());
    }

    @Test
    void shouldSupportUnaryOperatorsWithoutValue() {
        // NULL / NOT_NULL 不取值，允许省略第三个元素（含尾部逗号写法）
        ConditionNode condition = (ConditionNode) FilterParser.parse("[[name,NULL,]]");
        assertEquals("name", condition.field());
        assertEquals(OperateSymbol.NULL, condition.operator());
        assertTrue(condition.values().isEmpty());

        condition = (ConditionNode) FilterParser.parse("[[name,NOT_NULL]]");
        assertEquals(OperateSymbol.NOT_NULL, condition.operator());
        assertTrue(condition.values().isEmpty());
    }

    @Test
    void shouldReturnNullForEmptyStructuredFilter() {
        assertNull(FilterParser.parse("[[]]"));
    }

    @Test
    void shouldRejectMissingLeftOperand() {
        assertThrows(IllegalArgumentException.class,
                () -> FilterParser.parse("[[and,[[a,=,1]]]]"));
    }

    @Test
    void shouldRejectMissingRightOperand() {
        assertThrows(IllegalArgumentException.class,
                () -> FilterParser.parse("[[[a,=,1]],and]"));
    }

    @Test
    void shouldRejectInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> FilterParser.parse("a,=,1"));
    }
}
