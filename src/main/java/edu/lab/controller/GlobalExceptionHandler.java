package edu.lab.controller;

import jakarta.servlet.http.HttpServletRequest;
import edu.lab.security.AuthInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/** 将后端异常转换为前端可直接展示的统一 JSON 结构。 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> handleStatus(ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status).body(error(status, exception.getReason(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception, HttpServletRequest request) {
        String fields = exception.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getField)
                .distinct()
                .collect(Collectors.joining("、"));
        return ResponseEntity.badRequest().body(error(
                HttpStatus.BAD_REQUEST, "请检查必填项：" + fields, request));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleIntegrity(
            DataIntegrityViolationException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error(
                HttpStatus.CONFLICT, "数据重复或仍被其他业务记录引用", request));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleMissingResource(
            NoResourceFoundException exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error(
                HttpStatus.NOT_FOUND, "请求的资源不存在", request));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        logger.error("处理请求 {} 时发生未预期异常", request.getRequestURI(), exception);
        return ResponseEntity.internalServerError().body(error(
                HttpStatus.INTERNAL_SERVER_ERROR, "系统处理失败，请稍后重试", request));
    }

    private ApiError error(HttpStatus status, String message, HttpServletRequest request) {
        return new ApiError(
                LocalDateTime.now(), status.value(), code(status), message, request.getRequestURI(),
                String.valueOf(request.getAttribute(AuthInterceptor.REQUEST_ID_ATTRIBUTE)));
    }

    private String code(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "VALIDATION_ERROR";
            case UNAUTHORIZED -> "AUTH_REQUIRED";
            case FORBIDDEN -> "ACCESS_DENIED";
            case NOT_FOUND -> "NOT_FOUND";
            case CONFLICT -> "DATA_CONFLICT";
            case TOO_MANY_REQUESTS -> "RATE_LIMITED";
            default -> status.is5xxServerError() ? "INTERNAL_ERROR" : "BUSINESS_ERROR";
        };
    }

    record ApiError(
            LocalDateTime timestamp, int status, String code, String message, String path, String requestId) {}
}
