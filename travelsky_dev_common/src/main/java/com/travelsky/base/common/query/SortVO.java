package com.travelsky.base.common.query;

import java.io.Serializable;

public class SortVO implements Serializable {

    private String[] columns;
    private Type direction;

    public String[] getColumns() {
        return columns;
    }

    public void setColumns(String[] columns) {
        this.columns = columns;
    }

    public Type getDirection() {
        return direction;
    }

    public void setDirection(Type direction) {
        this.direction = direction;
    }

    public enum Type {
        ASC, DESC,
    }
}
