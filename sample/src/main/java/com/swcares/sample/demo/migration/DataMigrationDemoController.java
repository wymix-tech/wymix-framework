package com.swcares.sample.demo.migration;

import com.swcares.sample.entity.SampleUserEntity;
import com.travelsky.base.repository.datamigration.annotations.DataMigration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * travelsky_dev_data_migration 示例。
 *
 * <p>data_migration 模块监听数据变更日志事件（{@code DataChangeLogEvent}），
 * 当变更实体标注了 {@link DataMigration} 时触发迁移回调。
 * 本示例返回当前标注了该注解的实体清单，便于验证是否生效；
 * 开关为 {@code travelsky.data-migration.enabled}（默认开启）。
 */
@RestController
@RequestMapping("demo/dataMigration")
@Tag(name = "data_migration 模块示例")
public class DataMigrationDemoController {

    @GetMapping("entities")
    @Operation(summary = "列出标注了 @DataMigration 的实体")
    public Map<String, Boolean> entities() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put(SampleUserEntity.class.getSimpleName(),
                SampleUserEntity.class.isAnnotationPresent(DataMigration.class));
        return result;
    }
}
