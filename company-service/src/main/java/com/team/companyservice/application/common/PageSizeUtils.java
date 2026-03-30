package com.team.companyservice.application.common;

public final class PageSizeUtils {

    private PageSizeUtils() {
    }

    public static int normalize(int size) {
        if (size == 10 || size == 30 || size == 50) {
            return size;
        }
        return 10;
    }
}