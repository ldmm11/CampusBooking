package com.cbooking.campusbooking.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    SUCCESS(0, HttpStatus.OK, "成功"),
    INVALID_PARAMETER(1001, HttpStatus.BAD_REQUEST, "请求参数有误"),
    ACCOUNT_EXISTS(1002, HttpStatus.BAD_REQUEST, "注册账号已存在"),
    RESOURCE_NOT_FOUND(1004, HttpStatus.NOT_FOUND, "资源不存在"),
    UNAUTHENTICATED(2001, HttpStatus.UNAUTHORIZED, "身份认证失败"),
    FORBIDDEN(2002, HttpStatus.FORBIDDEN, "无操作权限"),
    SLOT_OCCUPIED(3001, HttpStatus.BAD_REQUEST, "时段已被占用"),
    BOOKING_OVERLAP(3002, HttpStatus.BAD_REQUEST, "与已有预约时间重叠"),
    DAILY_LIMIT_REACHED(3003, HttpStatus.BAD_REQUEST, "已达每日预约上限"),
    SLOT_OUT_OF_RANGE(3004, HttpStatus.BAD_REQUEST, "时段已开始或超出预约日期范围"),
    BOOKING_BANNED(3005, HttpStatus.FORBIDDEN, "当前处于禁约期"),
    INVALID_STATE(3006, HttpStatus.BAD_REQUEST, "当前状态不允许此操作"),
    INTERNAL_ERROR(5000, HttpStatus.INTERNAL_SERVER_ERROR, "服务端异常");

    private final int code;
    private final HttpStatus status;
    private final String message;

    ErrorCode(int code, HttpStatus status, String message) {
        this.code = code;
        this.status = status;
        this.message = message;
    }

    public int code() { return code; }
    public HttpStatus status() { return status; }
    public String message() { return message; }
}
