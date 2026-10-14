package org.wymix.framework.common.query;

import org.wymix.framework.common.exception.BusinessException;
import org.apache.commons.lang3.ArrayUtils;

public enum OperateSymbol {

    AND("AND"),
    OR("OR"),
    EQ("="),
    NE("!=", "<>"),
    CONTAINS("CONTAINS", "LIKE"),
    LIKE_RIGHT("RIGHT", "LIKE_RIGHT"),
    LIKE_LEFT("LEFT", "LIKE_LEFT"),
    GT(">"),
    GE(">="),
    LT("<"),
    LE("<="),
    BETWEEN("BETWEEN"),
    IN("IN"),
    NOT_IN("NOT_IN", "NOTIN"),
    NULL("NULL", "ISNULL"),
    NOT_NULL("NOT_NULL", "NOTNULL"),
//    EXISTS("EXIST"),
//    NOT_EXISTS("NOT_EXISTS"),
    ;

    private String[] symbol;

    OperateSymbol(String... symbol) {
        this.symbol = symbol;
    }

    public static OperateSymbol of(String value) {
        for (OperateSymbol operateSymbol : OperateSymbol.values()) {
            if (operateSymbol.equalsSymbol(value.toUpperCase())) {
                return operateSymbol;
            }
        }
        throw new BusinessException("");
    }

    public boolean equalsSymbol(String symbol) {
        return ArrayUtils.contains(this.symbol, symbol);
    }

    public String[] getSymbol() {
        return symbol;
    }
}
