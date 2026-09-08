package com.swcares.sample;

import com.travelsky.base.common.consts.SystemConstant;
import com.travelsky.base.common.exception.BusinessException;
import com.travelsky.base.common.exception.LoginException;
import com.travelsky.base.common.query.DevQueryVO;
import com.travelsky.base.common.query.OperateSymbol;
import com.travelsky.base.common.result.ResultVO;
import com.travelsky.base.utils.CompressUtil;
import com.travelsky.base.utils.DateUtils;
import com.travelsky.base.utils.GlobalIdGenerator;
import com.travelsky.base.utils.LocalDateTimeUtils;
import com.travelsky.base.utils.ReflectUtils;
import com.travelsky.base.utils.StringUtils;
import com.travelsky.base.utils.UUID;
import com.travelsky.base.utils.json.JsonUtils;
import com.travelsky.base.utils.map.SafeConcurrentHashMap;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        Assertions.assertEquals(8, StringUtils.generateRandomString(8).length());
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

    @Test
    void testCompressUtil() throws Exception {
        String raw = "sample-compress-payload-0123456789-0123456789";
        Assertions.assertEquals(raw, CompressUtil.decompress(CompressUtil.compress(raw)));
    }

    @Test
    void testUuid() {
        UUID uuid = UUID.fastUUID();
        Assertions.assertNotNull(uuid.toString());
        // 简化形式去掉分隔符
        Assertions.assertEquals(32, uuid.toString(true).length());
    }

    @Test
    void testLocalDateTimeUtils() {
        LocalDateTime now = LocalDateTime.of(2026, 1, 1, 0, 0);
        Assertions.assertEquals("2026-01-01 00:00:00", LocalDateTimeUtils.format(now, DateUtils.YYYYMMDDHHMMSS_LINE));
        Assertions.assertEquals(now.plusHours(3), LocalDateTimeUtils.add(now, 3, ChronoUnit.HOURS));
        Assertions.assertEquals(60_000, LocalDateTimeUtils.betweenInMills(now, now.plusMinutes(1)));
    }

    @Test
    void testReflectUtils() {
        Assertions.assertEquals(String.class, ReflectUtils.getSuperClassGenericType(GenericHolder.class, 0));
        Assertions.assertEquals(Long.class, ReflectUtils.getSuperClassGenericType(GenericHolder.class, 1));
        Assertions.assertEquals("", ReflectUtils.newInstance(GenericHolder.class, 0));
    }

    @Test
    void testSafeConcurrentHashMap() {
        SafeConcurrentHashMap<String, String> map = new SafeConcurrentHashMap<>();
        map.put("k", "v");
        Assertions.assertEquals("v", map.computeIfAbsent("k", key -> "recomputed"));
        Assertions.assertEquals("created", map.computeIfAbsent("missing", key -> "created"));
    }

    @Test
    void testExceptionSystem() {
        // BusinessException 默认携带系统内部错误码
        Assertions.assertEquals(SystemConstant.SYSTEM_INTERNAL_ERROR_CODE, new BusinessException("boom").getStatusCode());
        // LoginException 继承 BusinessException，由全局异常处理器映射为 401
        Assertions.assertInstanceOf(BusinessException.class, new LoginException("please login"));
    }

    static class GenericBase<A, B> {
    }

    static class GenericHolder extends GenericBase<String, Long> {
    }
}
