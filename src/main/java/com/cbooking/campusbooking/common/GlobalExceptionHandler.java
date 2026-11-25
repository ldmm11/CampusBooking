package com.cbooking.campusbooking.common;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> handleBusiness(BusinessException ex) {
        ErrorCode error = ex.errorCode();
        return ResponseEntity.status(error.status()).body(ApiResponse.of(error, null));
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ErrorCode error = switch (status.value()) {
            case 401 -> ErrorCode.UNAUTHENTICATED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            default -> status.is4xxClientError() ? ErrorCode.INVALID_PARAMETER : ErrorCode.INTERNAL_ERROR;
        };
        if (error == ErrorCode.INTERNAL_ERROR) {
            log.error("MVC 请求处理异常", ex);
        }
        ValidationDetails details = null;
        if (ex instanceof MethodArgumentNotValidException invalid) {
            var fields = invalid.getBindingResult().getFieldErrors().stream()
                    .map(field -> new FieldIssue(field.getField(), field.getDefaultMessage()))
                    .toList();
            if (!fields.isEmpty()) {
                details = new ValidationDetails(fields);
            }
        }
        return super.handleExceptionInternal(ex, ApiResponse.of(error, details), headers, error.status(), request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("未处理的服务端异常", ex);
        ErrorCode error = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(error.status()).body(ApiResponse.of(error, null));
    }

    private record ValidationDetails(List<FieldIssue> fieldErrors) {}
    private record FieldIssue(String field, String message) {}
}
