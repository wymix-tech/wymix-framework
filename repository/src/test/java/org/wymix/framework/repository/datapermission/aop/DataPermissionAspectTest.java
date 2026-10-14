package org.wymix.framework.repository.datapermission.aop;

import org.wymix.framework.repository.datapermission.annotations.DataPermission;
import org.wymix.framework.repository.datapermission.context.DataPermissionContextHolder;
import org.wymix.framework.repository.datapermission.enums.InjectType;
import org.wymix.framework.repository.datapermission.inject.PermissionSqlInjector;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DataPermissionAspect} 单元测试：验证「退出时恢复外层上下文，而非直接 clear」。
 *
 * <p>回归背景：若 {@code finally} 直接 {@code clear()}，嵌套 {@code @DataPermission}
 * 或两个切面实例同时环绕时，内层退出会清掉外层上下文，数据权限条件丢失
 * —— 静默返回全量数据，比抛异常更危险。
 *
 * <p>同时覆盖「切点改用纯类型匹配」这一修复：参数绑定式切点（{@code @annotation(dataPermission)}）
 * 在 JDK 动态代理下会抛 {@code Required to bind 2 arguments, but only bound 1}，
 * 这里通过只传 {@link ProceedingJoinPoint} 的调用方式固化该契约。
 */
class DataPermissionAspectTest {

    static class OuterInjector implements PermissionSqlInjector {
        @Override
        public Map<String, Object> additionalParameters() {
            return Map.of();
        }

        @Override
        public String injectSql() {
            return "OUTER = 1";
        }

        @Override
        public InjectType type() {
            return InjectType.WHERE_ADDITIONAL;
        }
    }

    static class InnerInjector implements PermissionSqlInjector {
        @Override
        public Map<String, Object> additionalParameters() {
            return Map.of();
        }

        @Override
        public String injectSql() {
            return "INNER = 1";
        }

        @Override
        public InjectType type() {
            return InjectType.WHERE_ADDITIONAL;
        }
    }

    /** 目标接口：方法上标注 {@code @DataPermission}，用于给切面提供真实的方法签名。 */
    interface Target {

        @DataPermission(injectors = {InnerInjector.class})
        void run();

        void runWithoutPermission();
    }

    @AfterEach
    void tearDown() {
        DataPermissionContextHolder.clear();
    }

    @Test
    void shouldMarkContextDuringProceedAndClearAfterwards() throws Throwable {
        new DataPermissionAspect().around(joinPoint("run", () ->
                assertTrue(DataPermissionContextHolder.exists())));
        // 无外层上下文：退出后清理干净
        assertFalse(DataPermissionContextHolder.exists());
    }

    @Test
    void shouldProceedWithoutContextWhenNoAnnotation() throws Throwable {
        new DataPermissionAspect().around(joinPoint("runWithoutPermission", () ->
                assertFalse(DataPermissionContextHolder.exists())));
        assertFalse(DataPermissionContextHolder.exists());
    }

    @Test
    void shouldRestoreOuterContextWhenNested() throws Throwable {
        DataPermissionContextHolder.DataPermissionContext outer = new DataPermissionContextHolder.DataPermissionContext();
        outer.setStatementIds(new String[]{"ALL"});
        outer.setInjectors(new java.util.ArrayList<>());
        outer.getInjectors().add(OuterInjector.class);
        DataPermissionContextHolder.mark(outer);

        // 内层切面执行：进入后写入自己的上下文，退出时必须恢复外层
        new DataPermissionAspect().around(joinPoint("run", () ->
                assertSame(InnerInjector.class, DataPermissionContextHolder.getContext().getInjectors().get(0))));

        assertTrue(DataPermissionContextHolder.exists(), "嵌套退出后外层上下文被清空，数据权限会静默失效");
        assertSame(outer, DataPermissionContextHolder.getContext());
    }

    @Test
    void shouldClearAndRethrowWhenProceedThrows() {
        assertThrows(IllegalStateException.class, () -> new DataPermissionAspect().around(joinPoint("run", () -> {
            throw new IllegalStateException("boom");
        })));
        assertFalse(DataPermissionContextHolder.exists());
    }

    /**
     * 构造一个 {@link ProceedingJoinPoint}：方法签名取自 {@link Target} 上对应名字的方法，
     * {@code proceed()} 执行传入动作。
     */
    private static ProceedingJoinPoint joinPoint(String methodName, Runnable action) throws Exception {
        Method method = Target.class.getMethod(methodName);
        return (ProceedingJoinPoint) Proxy.newProxyInstance(
                DataPermissionAspectTest.class.getClassLoader(),
                new Class<?>[]{ProceedingJoinPoint.class},
                new JoinPointStub(method, action));
    }

    static class JoinPointStub implements InvocationHandler {

        private final Method targetMethod;

        private final Runnable action;

        JoinPointStub(Method targetMethod, Runnable action) {
            this.targetMethod = targetMethod;
            this.action = action;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "proceed":
                    action.run();
                    return null;
                case "getSignature":
                    return signature();
                case "getTarget":
                    return new Object();
                case "getThis":
                    return new Object();
                case "getArgs":
                    return new Object[0];
                case "getKind":
                    return "method-execution";
                case "toShortString":
                case "toLongString":
                    return "stub";
                default:
                    return null;
            }
        }

        private Signature signature() {
            return (MethodSignature) Proxy.newProxyInstance(
                    DataPermissionAspectTest.class.getClassLoader(),
                    new Class<?>[]{MethodSignature.class},
                    (proxy, method, args) -> "getMethod".equals(method.getName()) ? targetMethod : null);
        }
    }
}
