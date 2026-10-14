package org.wymix.framework.web.configuration;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.*;

class TaskContextRestoreTest {
    @Test
    void callerRunsAndNestedDecoratorsRestoreExistingContextEvenOnFailure() {
        RequestAttributes caller = new ServletRequestAttributes(new MockHttpServletRequest());
        RequestAttributes task = new ServletRequestAttributes(new MockHttpServletRequest());
        RequestContextHolder.setRequestAttributes(caller);
        MDC.put("customTraceId", "caller");
        try {
            Runnable inner =
                    new WebConfiguration.WrappedRunnable(
                            () -> {
                                assertSame(task, RequestContextHolder.getRequestAttributes());
                                assertEquals("task", MDC.get("customTraceId"));
                                throw new IllegalStateException();
                            },
                            task,
                            "task");
            assertThrows(IllegalStateException.class, inner::run);
            assertSame(caller, RequestContextHolder.getRequestAttributes());
            assertEquals("caller", MDC.get("customTraceId"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
            MDC.clear();
        }
    }

    @Test
    void cleanWorkerContextRemainsCleanAfterTask() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
        new WebConfiguration.WrappedRunnable(
                        () -> {},
                        new ServletRequestAttributes(new MockHttpServletRequest()),
                        "task")
                .run();
        assertNull(RequestContextHolder.getRequestAttributes());
        assertNull(MDC.get("customTraceId"));
    }
}
