package org.wymix.framework.web.configuration;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.wymix.framework.utils.GlobalIdGenerator;
import org.wymix.framework.utils.StringUtils;
import org.wymix.framework.utils.json.NumberSerializer;
import org.wymix.framework.web.advice.ControllerResponseAdvice;
import org.wymix.framework.web.aop.RepeatRequestAspect;
import org.wymix.framework.web.aop.SlowRequestAspect;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskDecorator;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Validator;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Collectors;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    static {
        // 防止反射过度膨胀
        System.setProperty("sun.reflect.inflationThreshold", "2147483647");
    }

    @Autowired
    private Environment environment;

    /**
     * 传递请求上下文与链路 ID 到异步线程。
     *
     * <p>core 的 {@code AsyncScheduleAutoConfiguration} 也定义了同名的默认实现，
     * 此处声明 {@code @ConditionalOnMissingBean} 以保证两者共存时不发生 Bean 覆盖冲突。
     */
    @Bean
    @ConditionalOnMissingBean
    public TaskDecorator taskDecorator() {
        return (runnable -> {
            String customTraceId = ThreadContext.get("customTraceId");
            if (StringUtils.isBlank(customTraceId)) {
                customTraceId = GlobalIdGenerator.generateNextIdStr();
            }
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            return new WrappedRunnable(runnable, requestAttributes, customTraceId);
        });
    }

    @PostConstruct
    public void timeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(environment.getProperty("user.timezone", "GMT+08")));
    }

    @Bean
    ControllerResponseAdvice controllerResponseAdvice() {
        return new ControllerResponseAdvice();
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        List<MappingJackson2HttpMessageConverter> jackson2Converters = converters.stream().filter(e -> e instanceof MappingJackson2HttpMessageConverter)
                .map(e -> (MappingJackson2HttpMessageConverter) e).toList();
        jackson2Converters.forEach(e -> {
            SimpleModule module = new SimpleModule();
            // 解决前端精度问题
            module.addSerializer(Long.class, new NumberSerializer(Number.class));
            module.addSerializer(Number.class, new NumberSerializer(Number.class));
            e.getObjectMapper().registerModule(module);
        });
        converters.removeIf(e -> e instanceof MappingJackson2HttpMessageConverter);
        converters.addAll(0, jackson2Converters);
        List<ByteArrayHttpMessageConverter> byteArrayConverters = converters.stream().filter(e -> e instanceof ByteArrayHttpMessageConverter)
                .map(e -> (ByteArrayHttpMessageConverter) e).toList();
        converters.removeIf(e -> e instanceof ByteArrayHttpMessageConverter);
        converters.addAll(0, byteArrayConverters);
    }

    @Bean
    public Validator getValidator(@Value("${spring.validate.messages:i18n/validation/ValidationMessages}") String[] messagesResource) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        ResourceBundleMessageSource rbms = new ResourceBundleMessageSource();
        rbms.setDefaultEncoding("UTF-8");
        rbms.setBasenames(messagesResource);
        rbms.setDefaultLocale(Locale.US);
        validator.setValidationMessageSource(rbms);
        return validator;
    }

    @Bean
    public SlowRequestAspect slowRequestAspect() {
        return new SlowRequestAspect();
    }

    /**
     * {@code @RepeatRequest} 防重复提交切面。
     *
     * <p><b>默认不注册</b>（历史行为，去重粒度需业务确认）。两种启用方式（{@code 或} 的关系，二者取其一即可）：
     * <ul>
     *   <li>配置 {@code wymix.web.repeat-request.enabled=true} —— 注册本默认实现；</li>
     *   <li>业务方自行声明 {@code RepeatRequestAspect} Bean —— <b>无需配置开关</b>，
     *   仍会生效（本声明 {@code @ConditionalOnMissingBean}，自动让位）。</li>
     * </ul>
     *
     * <p>即：开关只控制「框架默认实现」是否注册，业务自声明的 Bean 永远优先，配不配开关都不受影响。
     *
     * <p>⚠️ 去重键存放在 {@code HttpSession}，仅同一会话内有效；无状态 / 分布式部署需自行改造为 Redis 等共享存储。
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "wymix.web.repeat-request", name = "enabled", havingValue = "true")
    public RepeatRequestAspect repeatRequestAspect() {
        return new RepeatRequestAspect();
    }

    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> setLogTraceIdFilter() {
        FilterRegistrationBean<OncePerRequestFilter> bean = new FilterRegistrationBean<>();
        bean.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                ThreadContext.put("customTraceId", GlobalIdGenerator.generateNextIdStr());
                try {
                    filterChain.doFilter(request, response);
                } finally {
                    ThreadContext.remove("customTraceId");
                }
            }
        });
        return bean;
    }

    static class WrappedRunnable implements Runnable {

        private final Runnable target;
        private final RequestAttributes requestAttributes;
        private final String customTraceId;

        public WrappedRunnable(Runnable target, RequestAttributes requestAttributes, String customTraceId) {
            this.target = target;
            this.requestAttributes = requestAttributes;
            this.customTraceId = customTraceId;
        }

        @Override
        public void run() {
            try {
                RequestContextHolder.setRequestAttributes(requestAttributes);
                ThreadContext.put("customTraceId", customTraceId);
                target.run();
            } finally {
                RequestContextHolder.resetRequestAttributes();
                ThreadContext.remove("customTraceId");
            }
        }
    }

}
