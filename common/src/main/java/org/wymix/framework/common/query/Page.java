package org.wymix.framework.common.query;

import java.io.Serializable;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Page<T> implements Serializable {

    private long page;

    private long size;

    private Collection<T> content;

    private long total;

    private long totalPage;

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Collection<T> getContent() {
        return content;
    }

    public void setContent(Collection<T> content) {
        this.content = content;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(long totalPage) {
        this.totalPage = totalPage;
    }

    public boolean isLast() {
        return page == totalPage;
    }

    public boolean isFirst() {
        return page == 1;
    }

    public boolean isHasNext() {
        return !isLast();
    }

    public boolean isHasPrev() {
        return !isFirst();
    }

    public boolean isEmpty() {
        return this.content == null || this.content.isEmpty();
    }

    public <V> Page<V> map(Function<? super T, ? extends V> function) {
        Page<V> vPage = new Page();
        vPage.setPage(this.page);
        vPage.setSize(this.size);
        vPage.setTotal(this.total);
        vPage.setTotalPage(this.totalPage);
        vPage.setContent(this.content.stream().map(function).collect(Collectors.toList()));
        return vPage;
    }
}
