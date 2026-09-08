package com.swcares.sample.dao;

import com.swcares.sample.entity.SampleUserEntity;
import com.travelsky.base.repository.jpa.api.BaseRepository;
import org.springframework.stereotype.Repository;

/**
 * JPA 版 DAO：继承 {@link BaseRepository}（内部已组合 {@code JpaRepository} +
 * {@code JpaSpecificationExecutor}），无需 {@code @MapperScan}，
 * 由 {@code @EnableJpaRepositories} 扫描注册（见同目录 DataSourceConfiguration）。
 */
@Repository
public interface SampleUserDao extends BaseRepository<SampleUserEntity, Long> {
}
