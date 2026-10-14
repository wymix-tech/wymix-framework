package com.swcares.sample.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.wymix.framework.repository.datachangelog.annotation.DataChangeLog;
import org.wymix.framework.repository.datachangelog.enums.StatementType;
import org.wymix.framework.repository.mybatis.audit.domain.AbstractAuditEntity;
import org.wymix.framework.repository.datamigration.annotations.DataMigration;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("SAMPLE_USER")
// repository 模块：标注后该实体的数据变更会记录变更日志（默认仅打印，可在 DataChangeLogEventListener 中覆写）
// record 指定记录哪些语句类型，缺省为 INSERT / MODIFY / REMOVE
@DataChangeLog(record = {StatementType.INSERT, StatementType.MODIFY, StatementType.REMOVE})
// data_migration 模块：标注后该实体的数据变更会触发迁移回调（开关 wymix.data-migration.enabled）
@DataMigration
public class SampleUserEntity extends AbstractAuditEntity implements Serializable {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "USERNAME")
    private String username;

    @TableField(value = "PASSWORD")
    private String password;

}
