package org.wymix.framework.security.context;

import org.wymix.framework.utils.map.SafeConcurrentHashMap;

import java.util.concurrent.ConcurrentMap;

public class JwtTokenBlackList {

    private final ConcurrentMap<String, Long> TOKEN_BLACK_LIST = new SafeConcurrentHashMap<>();

    /** Compatibility API: expireTime is a duration in milliseconds. */
    public void add(String token, long expireTime) {
        TOKEN_BLACK_LIST.put(token, System.currentTimeMillis() + expireTime);
    }

    /** Expiration is an absolute epoch millisecond value, derived from the signed JWT exp claim. */
    public void addUntil(String token, long expiresAtMillis) {
        long remaining = expiresAtMillis - System.currentTimeMillis();
        if (remaining > 0) {
            add(token, remaining);
        }
    }

    public boolean contains(String token) {
        clearExpiredTokens();
        return TOKEN_BLACK_LIST.containsKey(token);
    }

    private void clearExpiredTokens() {
        TOKEN_BLACK_LIST.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
    }

}
