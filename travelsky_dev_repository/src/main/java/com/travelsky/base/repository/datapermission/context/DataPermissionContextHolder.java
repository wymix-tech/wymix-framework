package com.travelsky.base.repository.datapermission.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;

import java.util.List;

/**
 * 数据权限上下文：由 {@code DataPermissionAspect} 在进入 {@code @DataPermission} 方法时写入，
 * 供各 ORM 实现侧（MyBatis 的 SQL 改写插件 / JPA 的 Predicate 追加）读取。
 *
 * <p>使用 TransmittableThreadLocal，异步线程（配合框架 TaskDecorator）中同样生效。
 */
public class DataPermissionContextHolder {

    private static final ThreadLocal<DataPermissionContext> DATA_PERMISSION_CONTEXT_THREAD_LOCAL = TransmittableThreadLocal.withInitial(() -> null);

    public static void mark(DataPermissionContext dataPermissionContext) {
        DATA_PERMISSION_CONTEXT_THREAD_LOCAL.set(dataPermissionContext);
    }

    public static void skip() {
        DATA_PERMISSION_CONTEXT_THREAD_LOCAL.remove();
    }

    public static boolean exists() {
        return DATA_PERMISSION_CONTEXT_THREAD_LOCAL.get() != null;
    }

    public static DataPermissionContext getContext() {
        return DATA_PERMISSION_CONTEXT_THREAD_LOCAL.get();
    }

    public static void clear() {
        DATA_PERMISSION_CONTEXT_THREAD_LOCAL.remove();
    }

    public static class DataPermissionContext {

        private String[] statementIds;

        private List<Class<? extends PermissionSqlInjector>> injectors;

        public String[] getStatementIds() {
            return statementIds;
        }

        public void setStatementIds(String[] statementIds) {
            this.statementIds = statementIds;
        }

        public List<Class<? extends PermissionSqlInjector>> getInjectors() {
            return injectors;
        }

        public void setInjectors(List<Class<? extends PermissionSqlInjector>> injectors) {
            this.injectors = injectors;
        }
    }

}
