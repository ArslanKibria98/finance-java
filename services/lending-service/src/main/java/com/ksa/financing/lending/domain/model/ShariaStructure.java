package com.ksa.financing.lending.domain.model;

public enum ShariaStructure {
    TAWARRUQ,
    MURABAHA,
    IJARA,
    MUSHARAKAH;

    public boolean requiresCommodity() {
        return this == TAWARRUQ;
    }
}
