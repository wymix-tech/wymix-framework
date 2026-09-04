package com.travelsky.base.repository.datachangelog.listener;

import com.travelsky.base.repository.datachangelog.annotation.DataChangeLog;
import com.travelsky.base.repository.datachangelog.dto.DataChangeLogDTO;
import com.travelsky.base.repository.datachangelog.event.DataChangeLogEvent;
import com.travelsky.base.utils.json.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;

public class DataChangeLogEventListener implements ApplicationListener<DataChangeLogEvent> {

    private static final Logger log = LoggerFactory.getLogger(DataChangeLogEventListener.class);

    @Override
    public void onApplicationEvent(DataChangeLogEvent event) {
        DataChangeLogDTO dataChangeLogDTO = event.getDataChangeLogDTO();
        try {
            Class clz = Class.forName(dataChangeLogDTO.getClassName());
            if (clz.isAnnotationPresent(DataChangeLog.class)) {
                changelog(dataChangeLogDTO);
            }
        } catch (ClassNotFoundException e) {
        }
    }

    protected void changelog(DataChangeLogDTO dataChangeLogDTO) {
        log.info("数据变更记录: {}", JsonUtils.entityToJson(dataChangeLogDTO));
    }
}
