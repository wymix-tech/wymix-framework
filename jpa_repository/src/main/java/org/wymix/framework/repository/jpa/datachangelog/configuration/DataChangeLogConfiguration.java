package org.wymix.framework.repository.jpa.datachangelog.configuration;

import org.wymix.framework.repository.datachangelog.listener.DataChangeLogEventListener;
import org.wymix.framework.repository.datachangelog.supports.BatchDeleteAspect;
import org.wymix.framework.repository.datachangelog.supports.DataChangeLogHandler;
import org.wymix.framework.repository.datachangelog.supports.DataChangeLogListener;
import org.wymix.framework.repository.datachangelog.supports.ManualDmlAspect;
import org.wymix.framework.repository.datachangelog.supports.ManualDmlDataChangeInterceptor;
import org.wymix.framework.utils.GlobalIdGenerator;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.persistence.EntityManagerFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@DependsOn("entityManagerFactory")
@ConditionalOnProperty(prefix = "data-changelog", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DataChangeLogConfiguration implements WebMvcConfigurer {

    static {
        System.setProperty("spring.jpa.properties.hibernate.ejb.interceptor", ManualDmlDataChangeInterceptor.class.getName());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        WebMvcConfigurer.super.addInterceptors(registry);
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                DataChangeLogListener.open(request.getRequestURI(), GlobalIdGenerator.generateNextId(), getClientIpAddress(request));
                return true;
            }
        }).addPathPatterns("/**");
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
                DataChangeLogListener.flush();
            }
        }).addPathPatterns("/**");
    }

    @Bean
    @DependsOn("auditorAware")
    public DataChangeLogHandler dataChangeLogHandler() {
        return new DataChangeLogHandler();
    }

    @Bean
    @DependsOn("dataChangeLogHandler")
    public DataChangeLogListener dataChangeLogListener() {
        return new DataChangeLogListener();
    }

    @Bean
    public ManualDmlAspect manualDmlAspect() {
        return new ManualDmlAspect();
    }

    @Bean
    public BatchDeleteAspect batchDeleteAspect() {
        return new BatchDeleteAspect();
    }

    @Bean
    @ConditionalOnMissingBean({DataChangeLogEventListener.class})
    public DataChangeLogEventListener auditEventListener() {
        return new DataChangeLogEventListener();
    }

    @Bean
    @DependsOn({"entityManagerFactory", "dataChangeLogListener"})
    Object addAuditListenerToJpaSessionFactory(@Autowired EntityManagerFactory entityManagerFactory, @Autowired ApplicationContext applicationContext) {
        DataChangeLogListener dataChangeLogListener = applicationContext.getBean(DataChangeLogListener.class);
        Object obj = new Object() {

            private Object init() {
                SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
                EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
                registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(dataChangeLogListener);

                registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(dataChangeLogListener);
                registry.getEventListenerGroup(EventType.POST_DELETE).appendListener(dataChangeLogListener);
                return this;
            }
        }.init();

        return obj;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        // 优先从代理头中获取
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            // X-Forwarded-For 可能包含多个IP（用逗号分隔），第一个为原始客户端IP
            String[] ips = xForwardedFor.split(",");
            for (String ip : ips) {
                ip = ip.trim();
                if (!ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                    // 过滤本地回环地址或内网地址（可选）
                    if (!ip.startsWith("127.") && !ip.startsWith("192.168.") && !ip.equals("10.0.0.0")) {
                        return ip;
                    }
                }
            }
        }

        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty() && !"unknown".equalsIgnoreCase(xRealIP)) {
            return xRealIP;
        }

        // 如果没有代理头，则使用远程地址
        return request.getRemoteAddr();
    }

}
