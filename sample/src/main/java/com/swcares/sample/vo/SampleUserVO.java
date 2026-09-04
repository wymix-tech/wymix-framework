package com.swcares.sample.vo;

import com.travelsky.base.common.vo.AbstractAuditVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "SampleUserVO")
public class SampleUserVO extends AbstractAuditVO implements Serializable {

    @Schema(description = "id")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

}
