package org.wymix.framework.security.handler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class DecryptEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String ENCRYPTED_PREFIX = "ENC(";
    private static final String ENCRYPTED_SUFFIX = ")";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {

        Map<String, Object> decryptedProperties = new HashMap<>();

        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource) {
                EnumerablePropertySource<?> enumerable = (EnumerablePropertySource<?>) source;

                for (String key : enumerable.getPropertyNames()) {
                    Object value = enumerable.getProperty(key);
                    if (value instanceof String) {
                        String strValue = (String) value;
                        if (isEncrypted(strValue)) {
                            // 解密
                            String decrypted = decrypt(strValue);
                            decryptedProperties.put(key, decrypted);
                        }
                    }
                }
            }
        }

        // 将解密后的属性添加到环境中
        if (!decryptedProperties.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource("decryptedProperties", decryptedProperties)
            );
        }
    }

    private boolean isEncrypted(String value) {
        return value.startsWith(ENCRYPTED_PREFIX) && value.endsWith(ENCRYPTED_SUFFIX);
    }

    private String decrypt(String encryptedValue) {
        // 去除 ENC() 包裹
        String ciphertext = encryptedValue.substring(
                ENCRYPTED_PREFIX.length(),
                encryptedValue.length() - ENCRYPTED_SUFFIX.length()
        );

        // 实现你的解密逻辑
        // 例如使用Jasypt、AES等
        return new String(Base64.getDecoder().decode(ciphertext), StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        // 确保在配置加载后但其他组件初始化前执行
        return Ordered.LOWEST_PRECEDENCE;
    }
}
