package com.travelsky.base.common.exception;

import com.travelsky.base.common.consts.SystemConstant;

import java.util.HashMap;
import java.util.Map;

public class BusinessException extends RuntimeException {

    private Integer statusCode;

    private String errorMessage;

    private Map<String, String> fieldErrors;

    private Object[] params;

    public BusinessException() {
        super(SystemConstant.SYSTEM_INTERNAL_ERROR);
        this.statusCode = SystemConstant.SYSTEM_INTERNAL_ERROR_CODE;
        this.errorMessage = SystemConstant.SYSTEM_INTERNAL_ERROR;
    }

    public BusinessException(String errorMessage) {
        super(errorMessage);
        this.statusCode = SystemConstant.SYSTEM_INTERNAL_ERROR_CODE;
        this.errorMessage = errorMessage;
    }

    public BusinessException(int statusCode, String errorMessage) {
        super(errorMessage);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
    }

    public BusinessException(String errorMessage, Map<String, String> fieldErrors) {
        super(errorMessage);
        this.statusCode = SystemConstant.SYSTEM_INTERNAL_ERROR_CODE;
        this.errorMessage = errorMessage;
        this.fieldErrors = fieldErrors;
    }

    public BusinessException(int statusCode, String errorMessage, Map<String, String> fieldErrors) {
        super(errorMessage);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.fieldErrors = fieldErrors;
    }

    public BusinessException(String errorMessage, Object... params) {
        super(errorMessage);
        this.statusCode = SystemConstant.SYSTEM_INTERNAL_ERROR_CODE;
        this.errorMessage = errorMessage;
        this.params = params;
    }

    public BusinessException(int statusCode, String errorMessage, Object... params) {
        super(errorMessage);
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;
        this.params = params;
    }

    public void addParam(String key, String value) {
        if (this.fieldErrors == null) {
            this.fieldErrors = new HashMap<>();
        }
        this.fieldErrors.put(key, value);
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public Object[] getParams() {
        return params;
    }
}
