package com.eason.ecom.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "PagedResponse", description = "Paginated response payload")
public record PagedResponse<T>(
        List<T> items,
        long totalElements,
        int totalPages,
        int page,
        int size) {
}
