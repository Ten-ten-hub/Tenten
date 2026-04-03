package com.team.common.page;

import java.util.List;
import org.springframework.data.domain.Page;

public record PageResponse<T>(
    List<T> content,
    PageInfo pageInfo
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
            page.getContent(),
            new PageInfo(
                page.getNumber() + 1,
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
            )
        );
    }

    public record PageInfo(
        int currentPage,
        int size,
        long totalElements,
        int totalPages
    ) {}
}
