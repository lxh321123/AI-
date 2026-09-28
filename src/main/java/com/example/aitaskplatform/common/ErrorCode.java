package com.example.aitaskplatform.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(400, "request parameter error"),
    NOT_FOUND(404, "resource not found"),
    TASK_STATUS_ERROR(409, "task status does not allow this operation"),
    IDEMPOTENT_CONFLICT(4091, "duplicate request is being processed"),
    TOO_MANY_RETRY(429, "task retry count exceeded"),
    SYSTEM_ERROR(500, "system error");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
