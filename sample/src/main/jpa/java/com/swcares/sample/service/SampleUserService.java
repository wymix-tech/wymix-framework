package com.swcares.sample.service;

import com.swcares.sample.dao.SampleUserDao;
import com.swcares.sample.entity.SampleUserEntity;
import com.travelsky.base.core.api.BaseService;
import com.travelsky.base.repository.datapermission.annotations.DataPermission;
import com.travelsky.base.repository.jpa.api.AbstractBaseServiceImpl;
import com.swcares.sample.permission.TestSqlInject;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * JPA 版 Service：与 mybatis 版共用 {@link BaseService} 契约，
 * 因此上层 Controller（{@code SampleUserController} 等）在两种 ORM 下无需改动。
 */
@Service
public class SampleUserService extends AbstractBaseServiceImpl<SampleUserEntity, Long, SampleUserDao> implements BaseService<SampleUserEntity, Long> {

    public SampleUserService(SampleUserDao repository) {
        super(repository);
    }

    /**
     * 与 mybatis 版保持同名方法以便上层示例复用。
     *
     * <p>JPA 侧数据权限：调用期间 {@code TestSqlInject} 声明的过滤条件会以
     * {@code Predicate} 形式追加到查询（see {@code SpecificationHelper#applyDataPermission}）。
     */
    @DataPermission(injectors = {TestSqlInject.class})
    public List<SampleUserEntity> findAllWithDataPermission() {
        return this.findAll();
    }

}
