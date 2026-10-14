package com.swcares.sample.demo.migration;

import com.swcares.sample.entity.SampleUserEntity;
import org.wymix.framework.repository.datamigration.annotations.DataMigration;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * data_migration 示例。
 *
 * <p>data_migration 模块监听数据变更日志事件（{@code DataChangeLogEvent}），
 * 当变更实体标注了 {@link DataMigration} 时触发迁移回调；
 * 模块自带的 {@code DataMigrationEventListener} 未实现回放（实验性），
 * 因此示例应用另注册了 {@link DemoDataMigrationListener} 接管动作并统计次数。
 * 开关为 {@code wymix.data-migration.enabled}（默认开启）。
 */
@RestController
@RequestMapping("demo/dataMigration")
@Tag(name = "data_migration 模块示例")
public class DataMigrationDemoController {

    private final DemoDataMigrationListener migrationListener;

    public DataMigrationDemoController(DemoDataMigrationListener migrationListener) {
        this.migrationListener = migrationListener;
    }

    @GetMapping("entities")
    @Operation(summary = "列出标注了 @DataMigration 的实体")
    public Map<String, Boolean> entities() {
        Map<String, Boolean> result = new LinkedHashMap<>();
        result.put(SampleUserEntity.class.getSimpleName(),
                SampleUserEntity.class.isAnnotationPresent(DataMigration.class));
        return result;
    }

    @GetMapping("stats")
    @Operation(summary = "迁移回调触发次数（对 SAMPLE_USER 做增删改后回来查看）")
    public Map<String, Long> stats() {
        Map<String, Long> result = new LinkedHashMap<>();
        result.put("migrated", migrationListener.getMigrated());
        result.put("skipped", migrationListener.getSkipped());
        return result;
    }
}
