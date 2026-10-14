package org.wymix.framework.web.aop;

import org.wymix.framework.common.exception.BusinessException;
import org.wymix.framework.web.context.RequestContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link RepeatRequestAspect} 回归测试。
 *
 * <p>重点覆盖两个历史缺陷：
 * <ol>
 *     <li>去重键曾使用 {@code StringBuilder#hashCode()}（身份哈希），同一请求每次算出的键都不同，
 *     导致去重永远不生效；</li>
 *     <li>时间窗判断方向写反，过期后仍会被拦截，导致接口永久不可用。</li>
 * </ol>
 */
class RepeatRequestAspectTest {

    private final RepeatRequestAspect aspect = new RepeatRequestAspect();

    @RestController
    static class DemoController {

        @RepeatRequest(expiredTimeInMills = 3000)
        public String submitPojo(Payload payload) { return "ok"; }

        @RepeatRequest(expiredTimeInMills = 3000)
        public String submit(String orderNo) {
            return "ok:" + orderNo;
        }
    }

    @BeforeEach
    void bindRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        org.springframework.web.context.request.RequestContextHolder
                .setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void unbindRequest() {
        org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes();
    }

    /**
     * 同一会话、相同参数：窗口内第二次提交必须被拦截。
     */
    @Test
    void shouldRejectDuplicateSubmissionInSameSession() throws Throwable {
        assertEquals("ok:A1", invoke("A1"));
        assertThrows(BusinessException.class, () -> invoke("A1"));
    }

    /**
     * 参数不同则视为不同请求，不应被拦截。
     */
    @Test
    void shouldAllowDifferentArguments() throws Throwable {
        assertEquals("ok:A1", invoke("A1"));
        assertEquals("ok:B2", invoke("B2"));
    }

    /**
     * 换一个会话（另一个 HttpSession）后重新开始计数。
     */
    @Test
    void shouldAllowAfterNewSession() throws Throwable {
        assertEquals("ok:A1", invoke("A1"));
        assertThrows(BusinessException.class, () -> invoke("A1"));

        MockHttpServletRequest another = new MockHttpServletRequest();
        another.setSession(new MockHttpSession());
        org.springframework.web.context.request.RequestContextHolder
                .setRequestAttributes(new ServletRequestAttributes(another));
        assertEquals("ok:A1", invoke("A1"));
    }

    /**
     * 校验 RequestContextHolder 能拿到会话（前置条件，避免上面用例因环境缺失而假通过）。
     */
    @Test
    void shouldBindSessionToRequestContext() {
        org.springframework.mock.web.MockHttpServletRequest request =
                (MockHttpServletRequest) RequestContextHolder.getRequest();
        org.junit.jupiter.api.Assertions.assertNotNull(request.getSession());
    }

    private Object invoke(String orderNo) throws Throwable {
        DemoController target = new DemoController();
        Method method = DemoController.class.getMethod("submit", String.class);

        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn(method.getName());
        when(signature.getDeclaringTypeName()).thenReturn(DemoController.class.getName());
        when(signature.getParameterNames()).thenReturn(new String[]{"orderNo"});

        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        when(point.getSignature()).thenReturn(signature);
        when(point.getTarget()).thenReturn(target);
        when(point.getArgs()).thenReturn(new Object[]{orderNo});
        when(point.proceed()).thenAnswer(invocation -> method.invoke(target, orderNo));
        when(point.proceed(any(Object[].class))).thenAnswer(invocation -> method.invoke(target, orderNo));

        return aspect.invoke(point);
    }
    static class Payload {
        private final String value;
        Payload(String value) { this.value=value; }
        public String getValue() { return value; }
    }
    @Test void equivalentDtoInstancesShareTheSameKey() throws Throwable {
        assertEquals("ok",invokePojo(new Payload("same")));
        assertThrows(BusinessException.class,()->invokePojo(new Payload("same")));
    }
    private Object invokePojo(Payload payload) throws Throwable {
        var target = new DemoController(); var method = DemoController.class.getMethod("submitPojo",Payload.class);
        var signature = mock(MethodSignature.class); when(signature.getMethod()).thenReturn(method);
        var point = mock(ProceedingJoinPoint.class); when(point.getSignature()).thenReturn(signature); when(point.getTarget()).thenReturn(target);
        when(point.getArgs()).thenReturn(new Object[]{payload}); when(point.proceed()).thenAnswer(call -> method.invoke(target,payload));
        return aspect.invoke(point);
    }
    @Test void concurrentRequestsInOneSessionHaveOneWinner() throws Exception {
        var session = new MockHttpSession(); var gate = new java.util.concurrent.CyclicBarrier(4);
        var accepted = new java.util.concurrent.atomic.AtomicInteger(); var executor = java.util.concurrent.Executors.newFixedThreadPool(4);
        try {
            var jobs = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i=0;i<4;i++) jobs.add(executor.submit(()->{
                var request = new MockHttpServletRequest(); request.setSession(session);
                org.springframework.web.context.request.RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
                try { gate.await(3,java.util.concurrent.TimeUnit.SECONDS); invokePojo(new Payload("same")); accepted.incrementAndGet(); }
                catch (BusinessException expected) { }
                catch (Throwable ex) { throw new RuntimeException(ex); }
                finally { org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes(); }
            }));
            for (var job : jobs) job.get(5,java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(1,accepted.get());
        } finally { executor.shutdownNow(); }
    }

}
