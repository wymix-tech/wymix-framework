package com.travelsky.base.data.migration.listener;

import com.travelsky.base.repository.datachangelog.dto.DataChangeLogDTO;
import com.travelsky.base.repository.datachangelog.event.DataChangeLogEvent;
import com.travelsky.base.repository.datamigration.annotations.DataMigration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;

public class DataMigrationEventListener implements ApplicationListener<DataChangeLogEvent> {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationEventListener.class);

    @Override
    public void onApplicationEvent(DataChangeLogEvent event) {
        DataChangeLogDTO dataChangeLogDTO = event.getDataChangeLogDTO();
        try {
            Class clz = Class.forName(dataChangeLogDTO.getClassName());
            if (clz.isAnnotationPresent(DataMigration.class)) {
                dataMigration(dataChangeLogDTO);
            }
        } catch (ClassNotFoundException e) {
            // 变更日志对应的实体类不在当前类路径（如跨模块场景），按设计忽略
            log.debug("DataMigration skipped: class not found: {}", dataChangeLogDTO.getClassName());
        }
    }

    /**
     * 实验性能力：暂未实现「变更日志 -> 迁移脚本」回放，调用前请确认模块定位。
     * <p>Experimental: not yet implemented, see README of travelsky_dev_data_migration.
     */
    private void dataMigration(DataChangeLogDTO dataChangeLogDTO) {
        // 实验性能力：迁移回放未实现。用 warn 级别提示（而非 debug），避免生产环境误以为迁移已生效
        log.warn("DataMigration replay is not implemented yet (experimental module), className={}",
                dataChangeLogDTO.getClassName());
    }
}
