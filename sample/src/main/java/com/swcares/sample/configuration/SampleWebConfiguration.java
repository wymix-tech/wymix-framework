package com.swcares.sample.configuration;

import com.travelsky.base.common.context.IgnoreUrlContext;
import com.travelsky.base.web.aop.RepeatRequestAspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * web / i18n 相关补充装配。
 *
 * <p>框架已自动装配的内容这里不再重复声明，只补充示例应用自身需要的两处：
 * <ol>
 *     <li>{@code AcceptHeaderLocaleResolver}：{@code MessageResourceHelper} 按类型获取该 Bean 解析
 *     请求语言（{@code Accept-Language}）。Boot 仅在未设置 {@code spring.web.locale} 时提供，
 *     此处显式声明保证 i18n 与全局异常文案渲染始终可用。Bean 名必须为 {@code localeResolver}。</li>
 *     <li>{@code IgnoreUrlContext}：向 security 模块贡献额外的放行路径，
 *     框架会收集容器内所有 {@code IgnoreUrlContext} 与 {@code security.ignoreUrls} 合并生效。
 *     其中 {@code /error} 必须放行——否则任何 404 转发到 {@code /error} 时会被鉴权拦截，
 *     表现为「明明是白名单路径却返回 401」。</li>
 *     <li>{@code RepeatRequestAspect}：web 模块默认未注册该切面（其构造需要业务方确认去重粒度），
 *     这里显式声明以启用 {@code @RepeatRequest}。去重键为「类+方法+参数」并存放在 HttpSession 中，
 *     因此同一会话内才生效。</li>
 * </ol>
 */
@Configuration
public class SampleWebConfiguration {

    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver localeResolver = new AcceptHeaderLocaleResolver();
        localeResolver.setDefaultLocale(Locale.SIMPLIFIED_CHINESE);
        return localeResolver;
    }

    @Bean
    public IgnoreUrlContext sampleIgnoreUrlContext() {
        IgnoreUrlContext ignoreUrlContext = new IgnoreUrlContext();
        ignoreUrlContext.setIgnoreUrls(new String[]{
                "/error",
                "/actuator/**",
                "/doc.html", "/api-docs.json", "/swagger-ui/**", "/webjars/**"});
        return ignoreUrlContext;
    }

    @Bean
    public RepeatRequestAspect repeatRequestAspect() {
        return new RepeatRequestAspect();
    }
}
