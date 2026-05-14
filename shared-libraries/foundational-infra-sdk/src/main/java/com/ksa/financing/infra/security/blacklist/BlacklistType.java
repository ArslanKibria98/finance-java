package com.ksa.financing.infra.security.blacklist;

import com.ksa.financing.infra.exception.ErrorCodes;

public enum BlacklistType {
    NID(ErrorCodes.Risk.BLACKLIST_NID),
    MOBILE(ErrorCodes.Risk.BLACKLIST_MOBILE),
    DEVICE(ErrorCodes.Risk.BLACKLIST_DEVICE),
    USER(ErrorCodes.Risk.BLACKLIST_USER),
    IP(ErrorCodes.Risk.BLACKLIST_IP);

    private final String errorCode;

    BlacklistType(String errorCode) {
        this.errorCode = errorCode;
    }

    public String errorCode() {
        return errorCode;
    }

    public String redisKey(String prefix, String valueHash) {
        return prefix + ":" + name().toLowerCase() + ":" + valueHash;
    }
}
