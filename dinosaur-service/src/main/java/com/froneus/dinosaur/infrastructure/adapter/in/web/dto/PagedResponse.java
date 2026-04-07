package com.froneus.dinosaur.infrastructure.adapter.in.web.dto;

import java.util.List;

/**
 * Response paginado — estructura del GET /v1/dinosaur:
 * {
 *   "data": [...],
 *   "meta": { "count": 2, "page": 1, "pageSize": 10 }
 * }
 */
public record PagedResponse<T>(
        List<T> data,
        Meta    meta
) {
    public record Meta(long count, int page, int pageSize) {}
}
