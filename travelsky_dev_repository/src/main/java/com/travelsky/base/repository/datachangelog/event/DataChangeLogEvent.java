package com.travelsky.base.repository.datachangelog.event;

import com.travelsky.base.repository.datachangelog.dto.DataChangeLogDTO;
import org.springframework.context.ApplicationEvent;

import java.time.Clock;

public class DataChangeLogEvent extends ApplicationEvent {

    private DataChangeLogDTO dataChangeLogDTO;


    public DataChangeLogEvent(Object source) {
        super(source);
    }

    public DataChangeLogEvent(Object source, Clock clock) {
        super(source, clock);
    }

    public DataChangeLogEvent(Object source, DataChangeLogDTO dataChangeLogDTO) {
        super(source);
        this.dataChangeLogDTO = dataChangeLogDTO;
    }

    public DataChangeLogDTO getDataChangeLogDTO() {
        return dataChangeLogDTO;
    }

}
