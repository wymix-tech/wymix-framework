package com.swcares.sample.demo.common;

import com.swcares.sample.entity.SampleUserEntity;
import com.swcares.sample.service.SampleUserService;
import com.swcares.sample.vo.SampleUserVO;
import org.wymix.framework.common.consts.SystemConstant;
import org.wymix.framework.common.context.Principal;
import org.wymix.framework.common.context.UserContextHolder;
import org.wymix.framework.common.exception.BusinessException;
import org.wymix.framework.common.exception.LoginException;
import org.wymix.framework.common.query.DevQueryVO;
import org.wymix.framework.common.query.OperateSymbol;
import org.wymix.framework.common.query.Page;
import org.wymix.framework.common.result.ResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * common 示例：统一响应 {@link ResultVO}、查询 DSL {@link DevQueryVO}、
 * 分页 {@link Page}、登录上下文 {@link UserContextHolder}、异常体系与错误码常量。
 */
@RestController
@RequestMapping("demo/common")
@Tag(name = "common 模块示例")
public class CommonDemoController {

    private final SampleUserService sampleUserService;

    public CommonDemoController(SampleUserService sampleUserService) {
        this.sampleUserService = sampleUserService;
    }

    @GetMapping("result")
    @Operation(summary = "ResultVO 统一响应封装")
    public ResultVO<Map<String, Object>> result() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("app", "sample");
        payload.put("statusCode", 200);
        // Controller 返回值会被 web 模块的 ControllerResponseAdvice 自动包装，这里演示手动包装
        ResultVO<Map<String, Object>> resultVO = ResultVO.of(payload);
        resultVO.addInvalidMessage("demo", "字段级错误信息示例");
        return resultVO;
    }

    @GetMapping("query")
    @Operation(summary = "DevQueryVO 过滤 DSL + Page 分页 + Page#map 转换")
    @SuppressWarnings("unchecked")
    public Page<SampleUserVO> query(@RequestParam(required = false) String username,
                                    @RequestParam(defaultValue = "1") Long page,
                                    @RequestParam(defaultValue = "10") Long limit) {
        DevQueryVO queryVO = new DevQueryVO();
        queryVO.setPage(page);
        queryVO.setLimit(limit);
        if (StringUtils.hasText(username)) {
            // 过滤条件形如 [["username","=","xxx"]]，字段名默认按驼峰转下划线映射列
            queryVO.addFilters(Triple.of("username", OperateSymbol.EQ, username));
        }
        Page<SampleUserEntity> entities = sampleUserService.findAll(queryVO);
        // Page#map 分页对象整体映射为 VO 分页，保留 total / totalPage 等分页元信息
        return entities.map(entity -> {
            SampleUserVO vo = new SampleUserVO();
            BeanUtils.copyProperties(entity, vo);
            return vo;
        });
    }

    @PostMapping("principal")
    @Operation(summary = "UserContextHolder 登录上下文（审计字段自动填充的数据来源）")
    public String principal(@RequestParam(defaultValue = "admin") String username) {
        Principal<String> principal = new Principal<>();
        principal.setPrincipal(username);
        principal.setSource(Principal.LoginSource.WEB);
        UserContextHolder.setContext(principal);

        Principal<String> current = UserContextHolder.getUserContext();
        return current.getPrincipal() + "/" + current.getSource();
    }

    @GetMapping("exception")
    @Operation(summary = "BusinessException 业务异常")
    public String exception() {
        throw new BusinessException("演示：业务异常会由全局异常处理器转为 ResultVO");
    }

    @GetMapping("exception/login")
    @Operation(summary = "LoginException 登录异常（全局处理器返回 401）")
    public String loginException() {
        throw new LoginException("demo.business.error");
    }

    @GetMapping("exception/params")
    @Operation(summary = "BusinessException 携带状态码、占位符参数与字段级错误")
    public String exceptionWithParams() {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        fieldErrors.put("username", "demo.valid.name");
        throw new BusinessException(400, "demo.business.error", fieldErrors);
    }

    @GetMapping("constants")
    @Operation(summary = "SystemConstant 框架错误码常量")
    public Map<String, Integer> constants() {
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("SYSTEM_INTERNAL_ERROR_CODE", SystemConstant.SYSTEM_INTERNAL_ERROR_CODE);
        result.put("ERROR_CODE_DATA_VALIDATION", SystemConstant.ERROR_CODE_DATA_VALIDATION);
        result.put("ERROR_CODE_DATABASE_SQL", SystemConstant.ERROR_CODE_DATABASE_SQL);
        result.put("ERROR_CODE_DATABASE_CONSTRAINT_VIOLATION", SystemConstant.ERROR_CODE_DATABASE_CONSTRAINT_VIOLATION);
        result.put("ERROR_CODE_VALIDATION_MESSAGE_NOT_READABLE", SystemConstant.ERROR_CODE_VALIDATION_MESSAGE_NOT_READABLE);
        return result;
    }
}
