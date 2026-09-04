package com.travelsky.base.dynamic.datasource.support;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DynamicEnvPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {

        MutablePropertySources propertySources = environment.getPropertySources();
        Iterator<PropertySource<?>> iterator = propertySources.iterator();
        HashMap<String, HashMap<String,Object>> result = new HashMap<>();
        while (iterator.hasNext()) {
            PropertySource<?> ps = iterator.next();
            if (ps instanceof EnumerablePropertySource) {
                EnumerablePropertySource eps = (EnumerablePropertySource) ps;
                String[] propertyNames = eps.getPropertyNames();
                for (String name : propertyNames) {
                    if (name.startsWith("dynamic.datasource.items.")) {
                        String temp = name.replaceAll("dynamic.datasource.items.", "");
                        String key = temp.substring(0, temp.indexOf("."));
                        if (result.containsKey(key)) {
                            result.get(key).put(temp.substring(temp.indexOf(".") + 1), eps.getProperty(name));
                        }else{
                            HashMap<String,Object> map = new HashMap<>();
                            result.put(key, map);
                            map.put(temp.substring(temp.indexOf(".") + 1), eps.getProperty(name));
                        }
                    }
                }
            }
        }

        HashMap<String, Object> source = new HashMap<>();
        for (Map.Entry<String, HashMap<String, Object>> entry : result.entrySet()) {
            source.put("dynamic.datasource.items." + entry.getKey()+".name", entry.getKey());
            source.put("dynamic.datasource.items." + entry.getKey()+".jdbcUrl", entry.getValue().get("url"));
            source.put("dynamic.datasource.items." + entry.getKey()+".poolName", entry.getKey());
        }
        MapPropertySource propertySource = new MapPropertySource("dynamic-datasource", source);

        propertySources.addLast(propertySource);
    }
}
