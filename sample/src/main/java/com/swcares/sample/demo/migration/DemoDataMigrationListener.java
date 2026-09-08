package com.swcares.sample.demo.migration;

import com.travelsky.base.repository.datachangelog.dto.DataChangeLogDTO;
import com.travelsky.base.repository.datachangelog.event.DataChangeLogEvent;
import com.travelsky.base.repository.datamigration.annotations.DataMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * travelsky_dev_data_migration 示例：自定义迁移回调。
 *
 * <p>data_migration 模块自带的 {@code DataMigrationEventListener} 仅判断实体是否标注
 * {@link DataMigration}，回放逻辑未实现（实验性）。业务方可像本类一样直接实现
 * {@link ApplicationListener}{@code <DataChangeLogEvent>} 接管迁移动作。
 *
 * <p>本示例只做计数与日志，控制器 {@code /demo/dataMigration/stats} 可查看累计次数。
 */
@Component
public class DemoDataMigrationListener implements ApplicationListener<DataChangeLogEvent> {

    private static final Logger log = LoggerFactory.getLogger(DemoDataMigrationListener.class);

    private final AtomicLong migrated = new AtomicLong();

    private final AtomicLong skipped = new AtomicLong();

    @Override
    public void onApplicationEvent(DataChangeLogEvent event) {
        DataChangeLogDTO dto = event.getDataChangeLogDTO();
        Class<?> clz;
        try {
            clz = Class.forName(dto.getClassName());
        } catch (ClassNotFoundException e) {
            skipped.incrementAndGet();
            return;
        }
        if (!clz.isAnnotationPresent(DataMigration.class)) {
            skipped.incrementAndGet();
            return;
        }
        migrated.incrementAndGet();
        log.info("[demo] 触发数据迁移 className={}, table={}, type={}, fields={}",
                dto.getClassName(), dto.getTableName(), dto.getStatementType(), dto.getFieldInfos().size());
    }

    public long getMigrated() {
        return migrated.get();
    }

    public long getSkipped() {
        return skipped.get();
    }
}
