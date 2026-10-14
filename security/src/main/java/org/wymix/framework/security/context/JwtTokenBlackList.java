package org.wymix.framework.security.context;

import org.wymix.framework.utils.map.SafeConcurrentHashMap;

import java.util.concurrent.ConcurrentMap;

public class JwtTokenBlackList {

    private static final ConcurrentMap<String, Long> TOKEN_BLACK_LIST = new SafeConcurrentHashMap<>();

    public void add(String token, long expireTime) {
        TOKEN_BLACK_LIST.put(token, System.currentTimeMillis() + expireTime);
    }

    public boolean contains(String token) {
        clearExpiredTokens();
        return TOKEN_BLACK_LIST.containsKey(token);
    }

    private void clearExpiredTokens() {
        TOKEN_BLACK_LIST.entrySet().removeIf(entry -> entry.getValue() < System.currentTimeMillis());
    }

}
