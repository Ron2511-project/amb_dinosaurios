package com.froneus.dinosaur.domain.model;

import java.util.List;

/**
 * Resultado paginado genérico del dominio.
 * Contiene los datos y metadatos de paginación (count, page, pageSize).
 */
public record PagedResult<T>(
        List<T> data,
        long    count,
        int     page,
        int     pageSize
) {}
