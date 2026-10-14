package org.wymix.framework.core.configuration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.wymix.framework.tp.configuration.ThreadPoolConfiguration;

import java.util.concurrent.Callable;

class MvcTaskExecutorTest {
    @Test
    void callableControllerUsesTheConfiguredApplicationExecutor() {
        new WebApplicationContextRunner()
                .withConfiguration(
                        AutoConfigurations.of(
                                AsyncScheduleAutoConfiguration.class, ThreadPoolConfiguration.class,
                                TaskExecutionAutoConfiguration.class,
                                        WebMvcAutoConfiguration.class))
                .withUserConfiguration(ControllerConfiguration.class)
                .withPropertyValues("spring.task.execution.thread-name-prefix=mvc-")
                .run(
                        ctx -> {
                            assertNull(ctx.getStartupFailure());
                            var mvc =
                                    MockMvcBuilders.webAppContextSetup(
                                                    ctx.getSourceApplicationContext())
                                            .build();
                            var result = mvc.perform(get("/callable")).andReturn();
                            assertTrue(result.getRequest().isAsyncStarted());
                            assertTrue(result.getAsyncResult(2000).toString().startsWith("mvc-"));
                        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ControllerConfiguration {
        @Bean
        DemoController controller() {
            return new DemoController();
        }
    }

    @RestController
    static class DemoController {
        @GetMapping("/callable")
        Callable<String> thread() {
            return () -> Thread.currentThread().getName();
        }
    }
}
