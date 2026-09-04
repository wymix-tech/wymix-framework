package com.travelsky.base.web.configuration;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.travelsky.base.core.configuration.AbstractAsyncScheduleConfiguration;
import com.travelsky.base.utils.GlobalIdGenerator;
import com.travelsky.base.utils.StringUtils;
import com.travelsky.base.utils.json.NumberSerializer;
import com.travelsky.base.web.advice.ControllerResponseAdvice;
import com.travelsky.base.web.aop.SlowRequestAspect;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class WebConfiguration extends AbstractAsyncScheduleConfiguration implements WebMvcConfigurer {

    static {
        // 防止反射过度膨胀
        System.setProperty("sun.reflect.inflationThreshold", "2147483647");
    }

    @Autowired
    private Environment environment;

    @Bean
    @Override
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

//    @Bean
//    public RepeatRequestAspect repeatRequestAspect() {
//        return new RepeatRequestAspect();
//    }

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
