package com.example.currencyconverter;

public class Currency {
    private final String code;
    private final String name;
    private final String symbol;
    private final String flag;

    public Currency(String code, String name, String symbol, String flag) {
        this.code = code;
        this.name = name;
        this.symbol = symbol;
        this.flag = flag;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getFlag() {
        return flag;
    }

    public String getDisplayString() {
        return flag + " " + code;
    }

    public String getDropdownString() {
        return flag + " " + code + " - " + name;
    }
}
