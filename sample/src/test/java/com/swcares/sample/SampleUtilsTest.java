package com.swcares.sample;

import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.common.query.OperateSymbol;
import com.travelsky.base.common.result.ResultVO;
import com.travelsky.base.utils.GlobalIdGenerator;
import com.travelsky.base.utils.StringUtils;
import com.travelsky.base.utils.json.JsonUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 不依赖外部环境的示例测试，演示 common / utils 模块的用法。
 */
class SampleUtilsTest {

    @Test
    void testJsonUtils() {
        String json = JsonUtils.entityToJson(Map.of("name", "sample"));
        Assertions.assertEquals("sample", JsonUtils.jsonToEntity(Map.class, json).get("name"));
    }

    @Test
    void testGlobalIdGenerator() {
        long first = GlobalIdGenerator.generateNextId();
        long second = GlobalIdGenerator.generateNextId();
        Assertions.assertTrue(second > first);
    }

    @Test
    void testStringUtils() {
        // camelToUnderline 同时转为大写，与 QueryParseHelper 的列映射规则一致
        Assertions.assertEquals("USER_NAME", StringUtils.camelToUnderline("userName"));
        Assertions.assertEquals("userName", StringUtils.underlineToCamel("user_name"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testDevQueryVO() {
        DevQueryVO queryVO = new DevQueryVO();
        queryVO.addFilters(Triple.of("username", OperateSymbol.EQ, "admin"));
        Assertions.assertEquals("[[username,=,admin]]", queryVO.getFilter());
        // 未显式设置时分页默认值
        Assertions.assertEquals(1L, queryVO.getPage());
        Assertions.assertEquals(20L, queryVO.getLimit());
    }

    @Test
    void testResultVO() {
        ResultVO<String> resultVO = ResultVO.of("ok");
        Assertions.assertEquals(200, resultVO.getStatusCode());
        Assertions.assertEquals("ok", resultVO.getPayload());
    }
}
