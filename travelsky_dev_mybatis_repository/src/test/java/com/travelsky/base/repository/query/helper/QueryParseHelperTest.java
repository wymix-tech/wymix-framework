package com.travelsky.base.repository.query.helper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.repository.query.helper.QueryParseHelper.ColumnNameStyleStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QueryParseHelper 单元测试：
 * 1. FilterStringParser 解析 DSL；
 * 2. 列名风格策略（原静态可变状态 CAMEL_TO_UNDERLINE 改为策略注入后行为兼容）；
 * 3. main 调试代码删除后 parseToQueryWrapper 行为不变。
 */
class QueryParseHelperTest {

    @AfterEach
    void restore() {
        QueryParseHelper.refresh();
    }

    static class User {
        private String userName;
        private Integer age;

        public String getUserName() {
            return userName;
        }

        public Integer getAge() {
            return age;
        }
    }

    @Test
    void filterStringParserShouldParseSimpleCondition() {
        List<Object> result = QueryParseHelper.FilterStringParser.parse("[[name,=,john]]");
        assertEquals(1, result.size());
        List<Object> condition = (List<Object>) result.get(0);
        assertEquals("name", condition.get(0));
        assertEquals("=", condition.get(1));
        assertEquals("john", condition.get(2));
    }

    @Test
    void filterStringParserShouldParseNestedCondition() {
        List<Object> result = QueryParseHelper.FilterStringParser.parse("[[name,=,john],or,[[age,>=,18]]]");
        assertEquals(3, result.size());
        assertInstanceOf(List.class, result.get(2));
    }

    @Test
    void filterStringParserShouldRejectInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> QueryParseHelper.FilterStringParser.parse("name=john"));
    }

    @Test
    void filterStringParserShouldTolerateUnclosedBracket() {
        // 既有容错行为：未闭合括号解析到末尾截止，不抛异常
        List<Object> result = QueryParseHelper.FilterStringParser.parse("[[name,=,john]");
        assertFalse(result.isEmpty());
    }

    @Test
    void parseToQueryWrapperShouldBuildCondition() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        DevQueryVO query = new DevQueryVO();
        query.setFilter("[[userName,=,john]]");
        QueryParseHelper.parseToQueryWrapper(wrapper, query, User.class);
        String sql = wrapper.getSqlSegment();
        assertTrue(sql.contains("USER_NAME = ") || sql.contains("userName = "),
                "actual: " + sql);
    }

    @Test
    void parseToQueryWrapperShouldReturnEmptyForEmptyQuery() {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        DevQueryVO query = new DevQueryVO();
        QueryParseHelper.parseToQueryWrapper(wrapper, query, User.class);
        assertTrue(wrapper.isEmptyOfWhere());
    }

    @Test
    void columnNameStyleStrategyShouldSupportInjection() {
        // 策略注入：驼峰不转下划线
        QueryParseHelper.setStrategy(ColumnNameStyleStrategy.camelToUnderline(false));
        assertEquals("userName", QueryParseHelper.formatToColumnName("userName"));

        // 策略注入：驼峰转下划线
        QueryParseHelper.setStrategy(ColumnNameStyleStrategy.camelToUnderline(true));
        assertEquals("USER_NAME", QueryParseHelper.formatToColumnName("userName"));
    }

    @Test
    void formatToColumnNameShouldKeepTableAlias() {
        QueryParseHelper.setStrategy(ColumnNameStyleStrategy.camelToUnderline(true));
        assertEquals("u.USER_NAME", QueryParseHelper.formatToColumnName("u.userName"));
    }

    @Test
    void refreshShouldFallbackToSystemProperty() {
        QueryParseHelper.setStrategy(ColumnNameStyleStrategy.camelToUnderline(false));
        QueryParseHelper.refresh();
        // 默认系统属性 COLUMN_NAME_RULE=CAMEL_TO_UNDERLINE -> true
        assertTrue(QueryParseHelper.isCamelToUnderline());
    }
}
