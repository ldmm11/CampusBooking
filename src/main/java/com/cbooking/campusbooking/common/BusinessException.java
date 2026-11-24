package com.cbooking.campusbooking.common;

import java.util.Objects;

public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(Objects.requireNonNull(errorCode).message());
        if (errorCode == ErrorCode.SUCCESS) {
            throw new IllegalArgumentException("业务异常必须使用错误码");
        }
        this.errorCode = errorCode;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }
}
