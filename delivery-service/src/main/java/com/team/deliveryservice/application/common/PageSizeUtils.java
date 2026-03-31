package com.team.deliveryservice.application.common;

import java.util.Set;

public final class PageSizeUtils {

    private static final Set<Integer> ALLOWED_PAGE_SIZES = Set.of(10, 30, 50);
    private static final int DEFAULT_PAGE_SIZE = 10;

    private PageSizeUtils() {
    }

    public static int normalize(Integer size) {
        if (size == null || !ALLOWED_PAGE_SIZES.contains(size)) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }
}
