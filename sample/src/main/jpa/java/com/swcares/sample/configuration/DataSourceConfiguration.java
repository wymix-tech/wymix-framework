package com.swcares.sample.configuration;

import com.travelsky.base.repository.datachangelog.dto.DataChangeLogDTO;
import com.travelsky.base.repository.datachangelog.listener.DataChangeLogEventListener;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA 版数据源装配。
 *
 * <p>与 mybatis 版不同，JPA 不需要 {@code @MapperScan}，改为：
 * <ul>
 *     <li>{@link EnableJpaRepositories}：扫描并注册 Spring Data Repository；</li>
 *     <li>{@link EntityScan}：显式声明实体所在包（示例包与主类同级，缺省也能扫到，此处显式声明以示用法）。</li>
 * </ul>
 * 数据变更日志同样由 jpa_repository 自动装配（{@code data-changelog.enabled}）。
 */
@Configuration
@EnableJpaRepositories(basePackages = {"com.swcares.sample.dao"})
@EntityScan(basePackages = {"com.swcares.sample.entity"})
public class DataSourceConfiguration {

    /**
     * 数据变更时，会自动推送事件，通过重写 changelog 方法，改变数据发生变更时对应的处理，默认只记录日志。
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
