package com.swcares.sample.demo.dynamicds;

import com.swcares.sample.entity.SampleUserEntity;
import com.swcares.sample.service.SampleUserService;
import org.wymix.framework.dynamic.datasource.annotations.DynamicDatasource;
import org.wymix.framework.dynamic.datasource.support.DynamicDataSourceContextHolder;
import org.springframework.stereotype.Service;

/**
 * dynamic_datasource 示例。
 *
 * <p>两种方式切换数据源：
 * <ol>
 *     <li>注解：{@link DynamicDatasource} 标注在类或方法上，AOP 在调用前后切换并清理；</li>
 *     <li>编程式：{@link DynamicDataSourceContextHolder#setDataSource(String)} 后需手动 {@code clear()}。</li>
 * </ol>
 * 需先开启 {@code dynamic.datasource.enabled=true} 并配置数据源清单，见 {@code application-dynamic-datasource.yml}。
 *
 * <p>本示例统一走 {@link SampleUserService}（{@code BaseService} 契约），
 * 因此在 MyBatis / JPA 两种 ORM 下都能直接运行。
 */
@Service
public class DynamicDatasourceDemoService {

    private final SampleUserService sampleUserService;

    public DynamicDatasourceDemoService(SampleUserService sampleUserService) {
        this.sampleUserService = sampleUserService;
    }

    /**
     * 走默认数据源（{@code dynamic.datasource.primary}）。
     */
    public long countOnPrimary() {
        return sampleUserService.findAll().size();
    }

    /**
     * 注解方式切到名为 slave 的数据源。
     */
    @DynamicDatasource("slave")
    public long countOnSlave() {
        return sampleUserService.findAll().size();
    }

    /**
     * 编程式切换：务必在 finally 中清理，避免线程复用导致数据源串扰。
     */
    public long countByContextHolder(String datasourceName) {
        try {
            DynamicDataSourceContextHolder.setDataSource(datasourceName);
            return sampleUserService.findAll().size();
        } finally {
            DynamicDataSourceContextHolder.clear();
        }
    }

    /**
     * 校验某个数据源是否已注册。
     */
    public boolean exists(String datasourceName) {
        return DynamicDataSourceContextHolder.containsDataSource(datasourceName);
    }

    public SampleUserEntity getOne(Long id) {
        return sampleUserService.getOne(id);
    }
}
