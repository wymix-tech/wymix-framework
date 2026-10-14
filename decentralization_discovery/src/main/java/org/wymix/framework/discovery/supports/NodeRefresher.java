/**
 * Copyright (c) 2025,Wymix.
 * All Rights Reserved.
 * Wymix CONFIDENTIAL
 * <p>
 * Project Name:dev_parent
 * Package Name:org.wymix.framework.discovery.supports
 * File Name:NodeRefresh
 * Date:2025/9/8 8:54
 */
package org.wymix.framework.discovery.supports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.wymix.framework.core.feign.supports.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * ClassName: NodeRefresh
 * Description: 
 * Date: 2025/9/8 8:54
 *
 * @author wang_yang
 */
public class NodeRefresher {

    private static final ObjectMapper MAPPER;

    private static final TypeReference<Map<String, List<NodeInfo>>> TYPE_REF =
            new TypeReference<Map<String, List<NodeInfo>>>() {
            };

    private static final Logger logger = LoggerFactory.getLogger(NodeRefresher.class);

    static {
        MAPPER = new ObjectMapper();
        // ✅ 注册 JavaTimeModule（支持 LocalDateTime 等）
        JavaTimeModule timeModule = new JavaTimeModule();

        // 🎯 可选：自定义时间格式（匹配你的 "yyyy-MM-dd HH:mm:ss"）
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        timeModule.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
        timeModule.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));

        MAPPER.registerModule(timeModule);

        // ✅ 关闭 WRITE_DATES_AS_TIMESTAMPS（避免输出为时间戳）
        MAPPER.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public static void refresh(String nodes) {
        try {
            Map<String, List<NodeInfo>> nodeMap = MAPPER.readValue(nodes, TYPE_REF);
            nodeMap.entrySet().stream().forEach((entry) -> {
                String serviceName = entry.getKey();
                List<NodeInfo> n = entry.getValue();
                for (NodeInfo nodeInfo : n) {
                    NodeRegistry.signUp(nodeInfo);
//                    logger.info("signUp: {} from NodeRefresher", nodeInfo.addr());
                }
            });
        } catch (Exception e) {
            logger.warn("Failed to refresh nodes", e);
        }

    }
}
