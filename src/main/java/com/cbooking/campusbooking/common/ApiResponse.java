package com.cbooking.campusbooking.common;

import org.slf4j.MDC;

public record ApiResponse<T>(int code, String message, T data, String traceId) {
    public static <T> ApiResponse<T> success(T data) {
        return of(ErrorCode.SUCCESS, data);
    }

    static <T> ApiResponse<T> of(ErrorCode result, T data) {
        return new ApiResponse<>(result.code(), result.message(), data, MDC.get(TraceIdFilter.KEY));
    }
}
