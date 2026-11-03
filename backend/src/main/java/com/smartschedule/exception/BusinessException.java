package com.smartschedule.exception;

/**
 * 业务异常 — 由 GlobalExceptionHandler 统一映射为 HTTP 400。
 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
