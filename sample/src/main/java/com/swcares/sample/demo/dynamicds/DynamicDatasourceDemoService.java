package com.swcares.sample.demo.dynamicds;

import com.swcares.sample.dao.SampleUserDao;
import com.swcares.sample.entity.SampleUserEntity;
import com.travelsky.base.dynamic.datasource.annotations.DynamicDatasource;
import com.travelsky.base.dynamic.datasource.support.DynamicDataSourceContextHolder;
import org.springframework.stereotype.Service;

/**
 * travelsky_dev_dynamic_datasource 示例。
 *
 * <p>两种方式切换数据源：
 * <ol>
 *     <li>注解：{@link DynamicDatasource} 标注在类或方法上，AOP 在调用前后切换并清理；</li>
 *     <li>编程式：{@link DynamicDataSourceContextHolder#setDataSource(String)} 后需手动 {@code clear()}。</li>
 * </ol>
 * 需先开启 {@code dynamic.datasource.enabled=true} 并配置数据源清单，见 {@code application-dynamic-datasource.yml}。
 */
@Service
public class DynamicDatasourceDemoService {

    private final SampleUserDao sampleUserDao;

    public DynamicDatasourceDemoService(SampleUserDao sampleUserDao) {
        this.sampleUserDao = sampleUserDao;
    }

    /**
     * 走默认数据源（{@code dynamic.datasource.primary}）。
     */
    public long countOnPrimary() {
        return sampleUserDao.selectCount(null);
    }

    /**
     * 注解方式切到名为 slave 的数据源。
     */
    @DynamicDatasource("slave")
    public long countOnSlave() {
        return sampleUserDao.selectCount(null);
    }

    /**
     * 编程式切换：务必在 finally 中清理，避免线程复用导致数据源串扰。
     */
    public long countByContextHolder(String datasourceName) {
        try {
            DynamicDataSourceContextHolder.setDataSource(datasourceName);
            return sampleUserDao.selectCount(null);
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
        return sampleUserDao.selectById(id);
    }
}
