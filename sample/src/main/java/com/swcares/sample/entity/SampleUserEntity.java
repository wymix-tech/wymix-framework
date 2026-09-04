package com.swcares.sample.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.travelsky.base.repository.audit.domain.AbstractAuditEntity;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("SAMPLE_USER")
public class SampleUserEntity extends AbstractAuditEntity implements Serializable {

    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(value = "USERNAME")
    private String username;

    @TableField(value = "PASSWORD")
    private String password;

}
