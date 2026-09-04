package com.travelsky.base.web.configuration;


import com.travelsky.base.common.context.IgnoreUrlContext;
import com.travelsky.base.security.properties.CustomSecurityProperties;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

public abstract class AbstractApiDocConfiguration {

    static {
        System.setProperty("springdoc.swagger-ui.path", "/doc.html");
        System.setProperty("springdoc.api-docs.path", "/api-docs.json");
    }

    @Bean
    @ConditionalOnProperty(prefix = "swagger.web", name = "enabled",havingValue = "true")
    public IgnoreUrlContext apiDocIgnoreUrlContext() {
        IgnoreUrlContext ignoreUrlContext = new IgnoreUrlContext();
        ignoreUrlContext.setIgnoreUrls(new String[]{"/doc.html", "/api-docs.json", "/swagger-ui/**", "/webjars/**", "/api-docs.json/**"});
        return ignoreUrlContext;
    }

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(apiDocTitle())
                        .description(apiDocDescription())
                        .version(apiDocVersion()));
    }

    @Bean
    protected String apiDocTitle() {
        return "Api Docs";
    }

    @Bean
    protected String apiDocDescription() {
        return "Api Docs";
    }

    @Bean
    protected String apiDocVersion() {
        return "1.0.0";
    }

    @Bean
    public GroupedOpenApi commonGroupOpenApi() {
        return GroupedOpenApi.builder()
                .group("default")
                .packagesToScan(packageToScan())
                .build();
    }

    @Bean
    protected abstract String[] packageToScan();


    @Configuration
    @ConditionalOnClass(CustomSecurityProperties.class)
    public static class AuthGroupOpenApiConfiguration {

        @Bean
        public OpenAPI securityOpenAPI(@Autowired String apiDocTitle, @Autowired String apiDocDescription, @Autowired String apiDocVersion) {
            return new OpenAPI()
                    .info(new Info()
                            .title(apiDocTitle)
                            .description(apiDocDescription)
                            .version(apiDocVersion))
                    .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                    .components(new Components()
                            .addSecuritySchemes("bearerAuth",
                                    new SecurityScheme()
                                            .name("bearerAuth")
                                            .type(SecurityScheme.Type.HTTP)
                                            .scheme("bearer")
                                            .bearerFormat("JWT")));
        }

        @Bean
        public GroupedOpenApi authGroupOpenApi(@Autowired(required = false) CustomSecurityProperties customSecurityProperties, @Autowired String[] packageToScan) {
            return GroupedOpenApi.builder()
                    .group("Auth")
                    .addOpenApiCustomizer(openApiCustomizer(customSecurityProperties))
                    .packagesToExclude(packageToScan)
                    .displayName("Auth")
                    .build();
        }

        public OpenApiCustomizer openApiCustomizer(CustomSecurityProperties customSecurityProperties) {
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
                String loginProcessingUrl = customSecurityProperties != null ? customSecurityProperties.getLoginProcessingUrl() : "/login";
                openApi.getPaths().addPathItem(loginProcessingUrl,
                        new io.swagger.v3.oas.models.Paths().get(loginProcessingUrl) != null
                                ? openApi.getPaths().get(loginProcessingUrl).post(loginOperation)
                                : new io.swagger.v3.oas.models.PathItem().post(loginOperation));
            };
        }
    }

}
