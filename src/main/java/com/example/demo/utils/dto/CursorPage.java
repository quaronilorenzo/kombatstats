package com.example.demo.utils.dto;

import java.util.List;

public record CursorPage<T, C>(
        List<T> items,
        C nextCursor,
        boolean hasNext
) {}
