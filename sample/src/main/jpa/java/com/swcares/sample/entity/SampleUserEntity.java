package com.swcares.sample.entity;

import org.wymix.framework.repository.datachangelog.annotation.DataChangeLog;
import org.wymix.framework.repository.datachangelog.enums.StatementType;
import org.wymix.framework.repository.datamigration.annotations.DataMigration;
import org.wymix.framework.repository.jpa.audit.domain.AbstractAuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * JPA 版实体（与 mybatis 版保持同一全限定名，便于上层示例代码在两种 ORM 间复用）。
 *
 * <p>审计字段（CREATED_BY / CREATED_TIME / LAST_MODIFIED_BY / LAST_MODIFIED_TIME）
 * 由 {@code jpa.audit.domain.AbstractAuditEntity} 提供，配合 {@code AuditListener}
 * 在保存/更新时自动填充，取值来源同样是 {@code UserContextHolder}。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "SAMPLE_USER")
// repository 模块：标注后该实体的数据变更会记录变更日志
@DataChangeLog(record = {StatementType.INSERT, StatementType.MODIFY, StatementType.REMOVE})
// data_migration 模块：标注后该实体的数据变更会触发迁移回调
@DataMigration
public class SampleUserEntity extends AbstractAuditEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "USERNAME")
    private String username;

    @Column(name = "PASSWORD")
    private String password;

}
