package com.swcares.sample.demo.repository;

import com.swcares.sample.entity.SampleUserEntity;
import com.travelsky.base.repository.datachangelog.annotation.DataChangeLog;
import com.travelsky.base.repository.datachangelog.enums.StatementType;
import com.travelsky.base.repository.datachangelog.plugin.DataChangeLogTableRegistry;
import com.travelsky.base.repository.datamigration.annotations.DataMigration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * travelsky_dev_repository 示例：数据变更日志（DataChangeLog）。
 *
 * <p>接入方式：在实体上标注 {@link DataChangeLog}（可选 {@code record} 指定记录哪些语句类型），
 * 并确保 {@code data.changelog.basePackages} 覆盖该实体所在包
 * （见 {@code application.yml}），{@code DataChangeLogTableRegistry} 启动时会扫描入注册表。
 *
 * <p>变更发生时 MyBatis 插件发布 {@code DataChangeLogEvent}，
 * 示例应用通过 {@code DataSourceConfiguration#dataChangeLogEventListener} 覆写处理逻辑，默认仅记录日志。
 */
@RestController
@RequestMapping("demo/changelog")
@Tag(name = "repository 数据变更日志示例")
public class DataChangeLogDemoController {

    @GetMapping("tables")
    @Operation(summary = "查看变更日志注册表（标注 @DataChangeLog / @DataMigration 的实体）")
    public Map<String, Object> tables() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sampleUserRegistered", DataChangeLogTableRegistry.exists("SAMPLE_USER"));
        result.put("sampleUserClassRegistered", DataChangeLogTableRegistry.exists(SampleUserEntity.class));
        result.put("resolvedTableByClass", DataChangeLogTableRegistry.exists(SampleUserEntity.class)
                ? DataChangeLogTableRegistry.getClass("SAMPLE_USER").getSimpleName() : null);
        return result;
    }

    @GetMapping("entities")
    @Operation(summary = "列出实体上声明的记录策略")
    public Map<String, Object> entities() {
        Map<String, Object> result = new LinkedHashMap<>();
        DataChangeLog dataChangeLog = SampleUserEntity.class.getAnnotation(DataChangeLog.class);
        result.put("entity", SampleUserEntity.class.getSimpleName());
        result.put("hasDataChangeLog", dataChangeLog != null);
        if (dataChangeLog != null) {
            List<String> record = Arrays.stream(dataChangeLog.record())
                    .map(StatementType::name)
                    .collect(Collectors.toList());
            result.put("record", record);
        }
        result.put("hasDataMigration", SampleUserEntity.class.isAnnotationPresent(DataMigration.class));
        return result;
    }
}
