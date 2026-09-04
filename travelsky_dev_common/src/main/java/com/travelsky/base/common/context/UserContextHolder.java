package com.travelsky.base.common.context;

import com.alibaba.ttl.TransmittableThreadLocal;

public class UserContextHolder {

    private static final ThreadLocal<Principal> CONTEXT = TransmittableThreadLocal.withInitial(() -> null);

    private UserContextHolder() {

    }

    public static void setContext(Principal principal) {
        CONTEXT.remove();
        CONTEXT.set(principal);
    }

    public static Principal getUserContext() {
        return CONTEXT.get();
    }

    public static void clearContext() {
        CONTEXT.remove();
    }

}
