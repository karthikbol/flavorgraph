package com.flavorgraph.backend.exception;

import com.flavorgraph.backend.dto.ApiDtos.ApiError;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiExceptions.DatabaseUnavailable.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ApiError database(ApiExceptions.DatabaseUnavailable ex) {
        log.warn("CognoDB operation failed: {}", ex.getCause() == null ? ex.getMessage() : ex.getCause().getClass().getSimpleName());
        return new ApiError("DATABASE_UNAVAILABLE", ex.getMessage(), Map.of());
    }

    @ExceptionHandler(ApiExceptions.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiError notFound(ApiExceptions.NotFound ex) { return new ApiError("NOT_FOUND", ex.getMessage(), Map.of()); }

    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError validation(Exception ex) {
        Map<String, String> details = new LinkedHashMap<>();
        if (ex instanceof MethodArgumentNotValidException invalid) {
            invalid.getBindingResult().getFieldErrors().forEach(e -> details.put(e.getField(), e.getDefaultMessage()));
        } else details.put("request", ex.getMessage());
        return new ApiError("VALIDATION_ERROR", "Please check the request and try again.", details);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiError unexpected(Exception ex) {
        log.error("Unexpected request failure", ex);
        return new ApiError("INTERNAL_ERROR", "FlavorGraph encountered an unexpected error.", Map.of());
    }
}
