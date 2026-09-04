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
        }
    }

    private void dataMigration(DataChangeLogDTO dataChangeLogDTO) {

    }
}
