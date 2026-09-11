package ${configPackage};

import com.travelsky.base.web.api.impl.AbstractBaseController;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Web 模块接入配置。
 *
 * <p>travelsky_dev_web 的 WebConfiguration / ApiDocAutoConfiguration 已自动装配：
 * 统一响应包装、TraceId 透传、Long 精度防护、i18n、慢请求切面与 Swagger 文档均已生效。
 *
 * <p>CRUD 控制器只需继承 AbstractBaseController 即可获得标准增删改查端点，
 * 无需在此声明任何 Bean；本文件仅演示两个最常见的可选定制点。
 */
@Configuration
public class OpenApiConfig {

    /**
     * 可选：覆盖框架默认 OpenAPI Bean（ApiDocAutoConfiguration#openAPI 标注了
     * {@code @ConditionalOnMissingBean}，声明即让位）。
     *
     * <p>注意：框架内部 Auth 分组的 securityOpenAPI 未加 {@code @ConditionalOnMissingBean}，
     * 声明本 Bean 不会覆盖它（见 ApiDocAutoConfiguration 说明）。
     */
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("${artifact}")
                .description("基于 travelsky_dev 框架生成的服务")
                .version("1.0.0"));
    }

    /**
     * 参考：业务 CRUD 控制器写法示例（复制为独立类使用，不要在配置类里定义控制器）。
     *
     * <pre>
     * // T=实体，V=VO，ID=主键类型，SERVICE 需继承框架 AbstractBaseServiceImpl
     * public class XxxController
     *         extends AbstractBaseController&lt;XxxEntity, XxxVO, Long, XxxService&gt; {
     *     // 覆写 mappedToVO / mappedToPO 可替换默认 VO↔PO 反射转换
     *     // 覆写 beforeSave / afterSave / beforeUpdate / afterUpdate 可挂业务钩子
     * }
     * </pre>
     *
     * @see AbstractBaseController
     */
    public void crudUsageNote() {
        // 仅作用法说明占位，无需实现
    }
}
