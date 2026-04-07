package com.haiapi.common.result;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private List<T> items;
    private long total;
    private int page;
    private int size;
    private int pages;
    private boolean hasPrevious;
    private boolean hasNext;

    public static <T> PageResult<T> of(List<T> items, long total, int page, int size) {
        PageResult<T> result = new PageResult<>();
        result.setItems(items);
        result.setTotal(total);
        result.setPage(page);
        result.setSize(size);
        result.setPages((int) Math.ceil((double) total / size));
        result.setHasPrevious(page > 1);
        result.setHasNext(page < result.getPages());
        return result;
    }
}
