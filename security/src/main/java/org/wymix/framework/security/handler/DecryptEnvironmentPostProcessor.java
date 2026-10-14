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

        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source instanceof EnumerablePropertySource<?> enumerable) {
                Map<String, Object> values = new HashMap<>();
                for (String key : enumerable.getPropertyNames()) {
                    Object value = enumerable.getProperty(key);
                    values.put(key, value instanceof String text && isEncrypted(text) ? decrypt(text) : value);
                }
                environment.getPropertySources().replace(source.getName(), new MapPropertySource(source.getName(), values));
            }
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

        // Compatibility encoding only: Base64 is not encryption.
        return new String(Base64.getDecoder().decode(ciphertext), StandardCharsets.UTF_8);
    }

    @Override
    public int getOrder() {
        // 确保在配置加载后但其他组件初始化前执行
        return Ordered.LOWEST_PRECEDENCE;
    }
}
