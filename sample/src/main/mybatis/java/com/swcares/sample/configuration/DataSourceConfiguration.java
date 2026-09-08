package com.swcares.sample.configuration;

import com.travelsky.base.repository.datachangelog.dto.DataChangeLogDTO;
import com.travelsky.base.repository.datachangelog.listener.DataChangeLogEventListener;
import com.travelsky.base.repository.datapermission.annotations.EnableDataPermission;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// 数据变更日志已随 mybatis_repository 自动装配（可用 data-changelog.enabled=false 关闭）
//开启全局数据权限
@EnableDataPermission(basePackages = {"com.swcares.sample.permission"})
@MapperScan(basePackages = {"com.swcares.sample.dao"})
public class DataSourceConfiguration {

    /**
     * 数据变更时，会自动推送事件，通过重写changelog方法，改变数据发生变更时对应的处理，默认只记录日志
     *
     * @return
     */
    @Bean
    public DataChangeLogEventListener dataChangeLogEventListener() {
        return new DataChangeLogEventListener() {
            @Override
            protected void changelog(DataChangeLogDTO dataChangeLogDTO) {
                super.changelog(dataChangeLogDTO);
            }
        };
    }
}
