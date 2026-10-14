package com.swcares.sample.demo.discovery;

import org.wymix.framework.discovery.properties.DecentralizedDiscoveryProperties;
import org.wymix.framework.discovery.supports.NodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * decentralization_discovery 示例：无注册中心的去中心化服务发现。
 *
 * <p>该能力默认关闭，需 {@code decentralization.discovery.enable=true} 并配置
 * {@code namespace} 与 {@code service}（见 {@code application-discovery.yml}）；
 * 因此本控制器同样按该开关条件装配，未开启时不会注册。
 */
@RestController
@RequestMapping("demo/discovery")
@Tag(name = "decentralization_discovery 模块示例")
@ConditionalOnProperty(prefix = "decentralization.discovery", name = "enable", havingValue = "true")
public class DiscoveryDemoController {

    private final NodeService nodeService;
    private final DecentralizedDiscoveryProperties properties;

    public DiscoveryDemoController(NodeService nodeService, DecentralizedDiscoveryProperties properties) {
        this.nodeService = nodeService;
        this.properties = properties;
    }

    @GetMapping("node")
    @Operation(summary = "查看本节点信息与集群命名空间")
    public Map<String, Object> node() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("selfNodeName", nodeService.getSelfNodeName());
        result.put("namespace", properties.getNamespace());
        result.put("serviceType", properties.type());
        result.put("services", properties.getService().keySet());
        return result;
    }
}
