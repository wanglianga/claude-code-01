package com.eldercare.common;

/** 业务校验异常，返回 400 */
public class ApiException extends RuntimeException {
    public ApiException(String message) {
        super(message);
    }
}
