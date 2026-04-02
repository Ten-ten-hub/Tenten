package com.team.common.page;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String value) {
        if (value == null || value.isBlank()) {
            return DESC;
        }

        try {
            return SortDirection.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DESC;
        }
    }
}
