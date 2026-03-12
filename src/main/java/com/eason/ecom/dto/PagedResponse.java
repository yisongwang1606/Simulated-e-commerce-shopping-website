package com.eason.ecom.dto;

import java.util.List;

public record PagedResponse<T>(
        List<T> items,
        long totalElements,
        int totalPages,
        int page,
        int size) {
}
