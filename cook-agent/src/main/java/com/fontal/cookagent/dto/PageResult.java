package com.fontal.cookagent.dto;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public record PageResult<T>(
        long page,
        long pageSize,
        long total,
        long pages,
        List<T> records
) {
    public static <T> PageResult<T> from(IPage<?> page, List<T> records) {
        return new PageResult<>(
                page.getCurrent(),
                page.getSize(),
                page.getTotal(),
                page.getPages(),
                records
        );
    }
}
