package com.travelsky.base.common.query;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;

import java.io.Serializable;

public class DevQueryVO implements Serializable {

    private String filter;

    private String sorts;

    private Long page = 1L;

    private Long limit = 20L;

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }


    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }

    public String getSorts() {
        return sorts;
    }

    public void setSorts(String sorts) {
        this.sorts = sorts;
    }

    public void addFilters(Triple<String, OperateSymbol, String>... filters) {
        if (filters == null || filters.length == 0) {
            return;
        }
        StringBuilder sb = new StringBuilder();

        sb.append("[");
        if (StringUtils.isNotBlank(this.filter)) {
            sb.append("[");
            sb.append(this.filter);
            sb.append("]");
        }
        Triple<String, OperateSymbol, String> triple;
        for (int i = 0; i < filters.length; i++) {
            triple = filters[i];
            sb.append("[").append(triple.getLeft()).append(",").append(triple.getMiddle().getSymbol()[0]).append(",").append(triple.getRight()).append("]");
            if (i != filters.length - 1) {
                sb.append(",");
            }
        }
        sb.append("]");
        this.filter = sb.toString();
    }
}
