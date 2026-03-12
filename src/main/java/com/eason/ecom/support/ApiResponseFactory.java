package com.eason.ecom.support;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.eason.ecom.dto.ApiResponse;

public final class ApiResponseFactory {

    private ApiResponseFactory() {
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data) {
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> ok(String message, T data) {
        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    public static ResponseEntity<ApiResponse<Void>> okMessage(String message) {
        return ResponseEntity.ok(ApiResponse.successMessage(message));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(String message, T data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, data));
    }
}
