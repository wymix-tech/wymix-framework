package com.travelsky.base.repository.datapermission.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.travelsky.base.repository.datapermission.inject.PermissionSqlInjector;

import java.util.List;

/**
 * 数据权限上下文：由 {@code DataPermissionAspect} 在进入 {@code @DataPermission} 方法时写入，
 * 供各 ORM 实现侧（MyBatis 的 SQL 改写插件 / JPA 的 Predicate 追加）读取。
 *
 * <p>使用 TransmittableThreadLocal，<b>仅</b>在被 TTL 包装的执行器（如 {@code TtlExecutors.getTtlExecutorService(...)）
 * 或 {@code TtlRunnable.get(runnable)} 包装过的任务中才会向子线程透传；框架 {@code WebConfiguration#taskDecorator}
 * 使用的是自定义 {@code WrappedRunnable}（只透传 traceId 与 RequestAttributes），<b>不会</b>透传本上下文，
 * 因此在 {@code @Async} / 自定义线程池中执行的数据权限会失效，需在线程内重新标注 {@code @DataPermission}。
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
