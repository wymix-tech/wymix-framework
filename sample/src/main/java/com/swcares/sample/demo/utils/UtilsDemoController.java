package com.swcares.sample.demo.utils;

import org.wymix.framework.utils.BeanUtils;
import org.wymix.framework.utils.CompressUtil;
import org.wymix.framework.utils.DateUtils;
import org.wymix.framework.utils.GlobalIdGenerator;
import org.wymix.framework.utils.HttpUtils;
import org.wymix.framework.utils.LocalDateTimeUtils;
import org.wymix.framework.utils.NetUtils;
import org.wymix.framework.utils.ReflectUtils;
import org.wymix.framework.utils.StringUtils;
import org.wymix.framework.utils.UUID;
import org.wymix.framework.utils.json.JsonUtils;
import org.wymix.framework.utils.map.SafeConcurrentHashMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * utils 示例：覆盖模块内全部工具类入口。
 */
@RestController
@RequestMapping("demo/utils")
@Tag(name = "utils 模块示例")
public class UtilsDemoController {

    @GetMapping("json")
    @Operation(summary = "JsonUtils 序列化/反序列化（含集合泛型与日期格式）")
    public Map<String, Object> json() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", GlobalIdGenerator.generateNextId());
        data.put("name", "sample");
        data.put("now", new Date());
        String json = JsonUtils.entityToJson(data);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("json", json);
        result.put("parsed", JsonUtils.jsonToEntity(Map.class, json));
        result.put("withDefaultDateFormat", JsonUtils.entityToJsonWithDefaultDateformat(data));
        return result;
    }

    @GetMapping("id")
    @Operation(summary = "GlobalIdGenerator 雪花算法 ID")
    public String id() {
        // 框架默认主键生成即使用该工具类（见 MyBatisConfiguration#getCustomIdGenerator）
        return GlobalIdGenerator.generateNextIdStr();
    }

    @GetMapping("string")
    @Operation(summary = "StringUtils 驼峰/下划线互转与随机串")
    public Map<String, String> string(@RequestParam(defaultValue = "userName") String value) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("camelToUnderline", StringUtils.camelToUnderline(value));
        result.put("underlineToCamel", StringUtils.underlineToCamel(StringUtils.camelToUnderline(value)));
        result.put("firstToUpperCase", StringUtils.firstToUpperCase(value));
        result.put("random", StringUtils.generateRandomString(8));
        return result;
    }

    @GetMapping("date")
    @Operation(summary = "DateUtils / LocalDateTimeUtils 日期时间处理")
    public Map<String, Object> date() {
        Map<String, Object> result = new LinkedHashMap<>();
        String text = DateUtils.format(new Date(), DateUtils.YYYYMMDDHHMMSS_LINE);
        result.put("dateFormat", text);
        result.put("dateParse", DateUtils.parse(text, DateUtils.YYYYMMDDHHMMSS_LINE));
        result.put("startOfDate", DateUtils.getStartOfDate(new Date()));
        result.put("endOfDate", DateUtils.getEndOfDate(new Date()));

        LocalDateTime now = LocalDateTime.now();
        result.put("ldtFormat", LocalDateTimeUtils.format(now, DateUtils.YYYYMMDDHHMMSS_LINE));
        result.put("ldtParsePlus1Day", LocalDateTimeUtils.parse(
                LocalDateTimeUtils.format(now, DateUtils.YYYYMMDDHHMMSS_LINE),
                DateUtils.YYYYMMDDHHMMSS_LINE));
        result.put("ldtAdd3Hours", LocalDateTimeUtils.add(now, 3, ChronoUnit.HOURS));
        result.put("ldtBetweenMillis", LocalDateTimeUtils.betweenInMills(now, now.plusMinutes(1)));
        result.put("ldtToDate", LocalDateTimeUtils.toDate(now));
        result.put("dateToLdt", LocalDateTimeUtils.toLocalDateTime(new Date()));
        return result;
    }

    @GetMapping("compress")
    @Operation(summary = "CompressUtil GZIP + Base64 压缩/解压")
    public Map<String, Object> compress(@RequestParam(defaultValue = "wymix_dev 框架压缩工具示例 payload") String raw) throws Exception {
        String compressed = CompressUtil.compress(raw);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("rawLength", raw.length());
        result.put("compressedLength", compressed.length());
        result.put("compressed", compressed);
        result.put("roundTripEqualsRaw", raw.equals(CompressUtil.decompress(compressed)));
        return result;
    }

    @GetMapping("uuid")
    @Operation(summary = "UUID 工具类（框架自带实现）")
    public Map<String, String> uuid() {
        UUID uuid = UUID.fastUUID();
        Map<String, String> result = new LinkedHashMap<>();
        result.put("fastUUID", uuid.toString());
        result.put("simple", uuid.toString(true));
        result.put("randomUUID", UUID.randomUUID().toString());
        return result;
    }

    @GetMapping("net")
    @Operation(summary = "NetUtils / HttpUtils 网络工具")
    public Map<String, Object> net(HttpServletRequest request) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("localhost", NetUtils.getLocalhostStr());
        result.put("clientIp", HttpUtils.getClientIpAddress(request));
        return result;
    }

    @GetMapping("reflect")
    @Operation(summary = "ReflectUtils 反射：泛型解析、字段读取、按注解查找字段")
    public Map<String, Object> reflect() {
        Map<String, Object> result = new LinkedHashMap<>();
        Class<?> entityType = ReflectUtils.getSuperClassGenericType(GenericHolder.class, 0);
        result.put("genericTypeAt0", entityType.getName());
        result.put("genericTypeAt1", ReflectUtils.getSuperClassGenericType(GenericHolder.class, 1).getName());
        result.put("newInstanceAt0", ReflectUtils.newInstance(GenericHolder.class, 0).getClass().getName());

        Field[] fields = ReflectUtils.findFieldWithAnnotation(Annotated.class, Deprecated.class);
        result.put("fieldsWithDeprecated", java.util.Arrays.stream(fields).map(Field::getName).toList());
        result.put("fieldValue", ReflectUtils.getFieldValue(new Annotated(), "name"));
        return result;
    }

    @GetMapping("concurrentMap")
    @Operation(summary = "SafeConcurrentHashMap：computeIfAbsent 的 key 级别加锁封装")
    public Map<String, Object> concurrentMap(@RequestParam(defaultValue = "sampleKey") String key) {
        SafeConcurrentHashMap<String, String> map = new SafeConcurrentHashMap<>();
        map.put(key, "value-of-" + key);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("size", map.size());
        // SafeConcurrentHashMap#computeIfAbsent 内部按 key 细粒度加锁，避免映射函数被并发重复执行
        result.put("computed", map.computeIfAbsent(key, k -> "computed"));
        result.put("fetched", map.computeIfAbsent("missing", k -> "created"));
        return result;
    }

    @GetMapping("bean")
    @Operation(summary = "BeanUtils 属性拷贝")
    public UtilsVO bean() {
        UtilsSource source = new UtilsSource();
        source.setId(1L);
        source.setName("sample");
        return BeanUtils.copyProperties(source, new UtilsVO());
    }

    static class GenericBase<A, B> {
    }

    static class GenericHolder extends GenericBase<String, Long> {
    }

    static class Annotated {
        @Deprecated
        private String legacy = "legacy-field";
        private String name = "sample";

        public String getName() {
            return name;
        }
    }

    public static class UtilsSource {
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class UtilsVO {
        private Long id;
        private String name;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
