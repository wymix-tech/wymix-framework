package com.swcares.sample.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.travelsky.base.repository.audit.domain.AbstractAuditEntity;
import com.travelsky.base.repository.datamigration.annotations.DataMigration;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("SAMPLE_USER")
// data_migration 模块：标注后该实体的数据变更会触发迁移回调（开关 travelsky.data-migration.enabled）
@DataMigration
public class SampleUserEntity extends AbstractAuditEntity implements Serializable {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "USERNAME")
    private String username;

    @TableField(value = "PASSWORD")
    private String password;

}
