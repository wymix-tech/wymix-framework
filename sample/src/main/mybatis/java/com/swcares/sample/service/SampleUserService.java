package com.swcares.sample.service;

import com.swcares.sample.dao.SampleUserDao;
import com.swcares.sample.entity.SampleUserEntity;
import com.swcares.sample.permission.TestSqlInject;
import org.wymix.framework.core.api.BaseService;
import org.wymix.framework.repository.mybatis.api.AbstractBaseServiceImpl;
import org.wymix.framework.repository.datapermission.annotations.DataPermission;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SampleUserService extends AbstractBaseServiceImpl<SampleUserEntity, Long, SampleUserDao> implements BaseService<SampleUserEntity, Long> {

    public SampleUserService(SampleUserDao repository) {
        super(repository);
    }

    /**
     * 数据权限示例：调用该方法期间的 SQL 会被 {@link TestSqlInject} 追加过滤条件。
     */
    @DataPermission(injectors = {TestSqlInject.class})
    public List<SampleUserEntity> findAllWithDataPermission() {
        return this.findAll();
    }

}
