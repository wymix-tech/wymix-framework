package org.wymix.framework.common.result;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * The type Result vo.
 *
 * @param <T> the type parameter
 * @author xnky_wangyang01
 * @version 1.0
 * @date 2025 -01-10
 * @copyright 中国航信
 */
public class ResultVO<T extends Object> implements Serializable {

    private Integer statusCode;

    private T payload;

    private String errorMessage;

    private Map<String, String> invalidMessage = new HashMap<>();

    public ResultVO() {

    }

    private <V extends T> ResultVO(V payload) {
        this.statusCode = 200;
        this.payload = payload;
    }

    private <V extends T> ResultVO(Integer statusCode, String errorMessage) {
        this.statusCode = statusCode;
        this.errorMessage = errorMessage;

    }

    /**
     * Of result vo.
     *
     * @param <V>     the type parameter
     * @param payload the payload
     * @return the result vo
     */
    public static <V extends Object> ResultVO of(V payload) {
        return new ResultVO(payload);
    }

    /**
     * Of result vo.
     *
     * @param statusCode   the status code
     * @param errorMessage the error message
     * @return the result vo
     */
    public static ResultVO of(int statusCode, String errorMessage) {
        return new ResultVO(statusCode, errorMessage);
    }

    /**
     * Add invalid message.
     *
     * @param field        the field
     * @param errorMessage the error message
     */
    public void addInvalidMessage(String field, String errorMessage) {
        this.invalidMessage.put(field, errorMessage);
    }

    /**
     * Gets status code.
     *
     * @return the status code
     */
    public Integer getStatusCode() {
        return statusCode;
    }

    /**
     * Sets status code.
     *
     * @param statusCode the status code
     */
    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    /**
     * Gets payload.
     *
     * @return the payload
     */
    public T getPayload() {
        return payload;
    }

    /**
     * Sets payload.
     *
     * @param payload the payload
     */
    public void setPayload(T payload) {
        this.payload = payload;
    }

    /**
     * Gets error message.
     *
     * @return the error message
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Sets error message.
     *
     * @param errorMessage the error message
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * Gets invalid message.
     *
     * @return the invalid message
     */
    public Map<String, String> getInvalidMessage() {
        return invalidMessage;
    }

    /**
     * Sets invalid message.
     *
     * @param invalidMessage the invalid message
     */
    public void setInvalidMessage(Map<String, String> invalidMessage) {
        this.invalidMessage = invalidMessage;
    }
}
