package com.swcares.sample.demo.utils;

import com.travelsky.base.utils.BeanUtils;
import com.travelsky.base.utils.DateUtils;
import com.travelsky.base.utils.GlobalIdGenerator;
import com.travelsky.base.utils.StringUtils;
import com.travelsky.base.utils.json.JsonUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * travelsky_dev_utils 示例：JsonUtils、GlobalIdGenerator、StringUtils、DateUtils、BeanUtils。
 */
@RestController
@RequestMapping("demo/utils")
@Tag(name = "utils 模块示例")
public class UtilsDemoController {

    @GetMapping("json")
    @Operation(summary = "JsonUtils 序列化/反序列化")
    public String json() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", GlobalIdGenerator.generateNextId());
        data.put("name", "sample");
        String json = JsonUtils.entityToJson(data);
        return JsonUtils.jsonToEntity(Map.class, json).toString();
    }

    @GetMapping("id")
    @Operation(summary = "GlobalIdGenerator 雪花算法 ID")
    public String id() {
        // 框架默认主键生成即使用该工具类（见 MyBatisConfiguration#getCustomIdGenerator）
        return GlobalIdGenerator.generateNextIdStr();
    }

    @GetMapping("string")
    @Operation(summary = "StringUtils 驼峰/下划线互转")
    public Map<String, String> string(@RequestParam(defaultValue = "userName") String value) {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("camelToUnderline", StringUtils.camelToUnderline(value));
        result.put("underlineToCamel", StringUtils.underlineToCamel(StringUtils.camelToUnderline(value)));
        result.put("random", StringUtils.generateRandomString(8));
        return result;
    }

    @GetMapping("date")
    @Operation(summary = "DateUtils 日期格式化与解析")
    public Map<String, Object> date() {
        Map<String, Object> result = new LinkedHashMap<>();
        String text = DateUtils.format(new Date(), DateUtils.YYYYMMDDHHMMSS_LINE);
        result.put("format", text);
        result.put("parse", DateUtils.parse(text, DateUtils.YYYYMMDDHHMMSS_LINE));
        return result;
    }

    @GetMapping("bean")
    @Operation(summary = "BeanUtils 属性拷贝")
    public UtilsVO bean() {
        UtilsSource source = new UtilsSource();
        source.setId(1L);
        source.setName("sample");
        UtilsVO target = BeanUtils.copyProperties(source, new UtilsVO());
        return target;
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
