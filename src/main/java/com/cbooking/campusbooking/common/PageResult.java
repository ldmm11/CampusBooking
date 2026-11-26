package com.cbooking.campusbooking.common;

import java.util.List;

public record PageResult<T>(List<T> items, long total, int page, int size) {
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
}
