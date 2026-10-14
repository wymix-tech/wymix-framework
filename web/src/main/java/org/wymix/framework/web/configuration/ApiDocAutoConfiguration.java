package org.wymix.framework.web.configuration;

import org.wymix.framework.common.context.IgnoreUrlContext;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * API 文档统一自动装配。
 * 引入 web 即生效，可通过 swagger.enabled=false 关闭。
 * 业务定制：
 * - swagger.title / swagger.description / swagger.version 配置文档信息
 * - springdoc.packages-to-scan 配置扫描包（逗号分隔），默认扫描主类所在包
 * - 声明 GroupedOpenApi Bean 即可完全自定义分组
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ApiDocAutoConfiguration {

    static {
        System.setProperty("springdoc.swagger-ui.path", "/doc.html");
        System.setProperty("springdoc.api-docs.path", "/api-docs.json");
    }

    @Autowired private org.springframework.core.env.Environment environment;

    @Value("${swagger.title:Api Docs}")
    private String apiDocTitle;

    @Value("${swagger.description:Api Docs}")
    private String apiDocDescription;

    @Value("${swagger.version:1.0.0}")
    private String apiDocVersion;

    @Value("${springdoc.packages-to-scan:}")
    private String[] packagesToScan;

    @Bean
    @ConditionalOnProperty(prefix = "swagger.web", name = "enabled", havingValue = "true")
    public IgnoreUrlContext apiDocIgnoreUrlContext() {
        IgnoreUrlContext ignoreUrlContext = new IgnoreUrlContext();
        ignoreUrlContext.setIgnoreUrls(new String[]{"/doc.html", "/api-docs.json", "/swagger-ui/**", "/webjars/**", "/api-docs.json/**"});
        return ignoreUrlContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI() {
        OpenAPI api = new OpenAPI().info(new Info().title(apiDocTitle).description(apiDocDescription).version(apiDocVersion));
        if (org.springframework.util.ClassUtils.isPresent("org.wymix.framework.security.properties.CustomSecurityProperties",getClass().getClassLoader())
                && environment.getProperty("security.enabled",Boolean.class,true)) {
            api.addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                    .components(new Components().addSecuritySchemes("bearerAuth",new SecurityScheme().name("bearerAuth")
                            .type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
        }
        return api;
    }

    @Bean
    @ConditionalOnMissingBean(GroupedOpenApi.class)
    public GroupedOpenApi commonGroupOpenApi() {
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder().group("default");
        if (packagesToScan != null && packagesToScan.length > 0 && !packagesToScan[0].isBlank()) {
            builder.packagesToScan(packagesToScan);
        }
        return builder.build();
    }

    @Configuration
    @ConditionalOnClass(name = "org.wymix.framework.security.properties.CustomSecurityProperties")
    @ConditionalOnProperty(prefix = "security", name = "enabled", havingValue = "true", matchIfMissing = true)
    public static class AuthGroupOpenApiConfiguration {

        @Bean
        @ConditionalOnMissingBean(name = "authGroupOpenApi")
        @ConditionalOnProperty(prefix = "security", name = "form-login-enable", havingValue = "true", matchIfMissing = true)
        public GroupedOpenApi authGroupOpenApi(@Value("${security.login-processing-url:/login}") String loginProcessingUrl,
                                               @Value("${springdoc.packages-to-scan:}") String[] packageToScan) {
            return GroupedOpenApi.builder()
                    .group("Auth")
                    .addOpenApiCustomizer(openApiCustomizer(loginProcessingUrl))
                    .packagesToExclude(packageToScan)
                    .displayName("Auth")
                    .build();
        }

        public OpenApiCustomizer openApiCustomizer(String loginProcessingUrl) {
            return openApi -> {
                ApiResponse successResponse = new ApiResponse().description("登录成功");
                ApiResponse unauthorizedResponse = new ApiResponse().description("用户名或密码错误");
                Operation loginOperation = new Operation()
                        .summary("用户登录")
                        .addTagsItem("Auth")
                        .description("提交用户名和密码进行认证")
                        .parameters(List.of(new Parameter().name("username").in("query").schema(new Schema<>().type("string").example("user"))
                                , new Parameter().name("password").in("query").schema(new Schema<>().type("string").example("123456"))))
                        .responses(new ApiResponses()
                                .addApiResponse("200", successResponse)  // 表单登录默认重定向
                                .addApiResponse("401", unauthorizedResponse));

                // 手动添加到 /login 路径（POST）
                if (openApi.getPaths() == null) openApi.setPaths(new io.swagger.v3.oas.models.Paths());
                openApi.getPaths().addPathItem(loginProcessingUrl,
                        new io.swagger.v3.oas.models.Paths().get(loginProcessingUrl) != null
                                ? openApi.getPaths().get(loginProcessingUrl).post(loginOperation)
                                : new io.swagger.v3.oas.models.PathItem().post(loginOperation));
            };
        }
    }

}
