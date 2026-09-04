package com.travelsky.base.core.boot;

import com.travelsky.base.core.listener.FeignApplicationStartedEventListener;
import com.travelsky.base.core.listener.LogApplicationStartedEventListener;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;

public class BootLoader {

    private static List<ApplicationListener<?>> listeners = new ArrayList<>();

    public static SpringApplication genApp(Class clazz) {
        SpringApplication app = new SpringApplication(clazz);
        addListener(new LogApplicationStartedEventListener());
        addListener(new FeignApplicationStartedEventListener());
        app.addListeners(BootLoader.listeners.stream().toArray(ApplicationListener<?>[]::new));
        return app;
    }

    public static void addListener(ApplicationListener<?> listener) {
        BootLoader.listeners.add(listener);
    }


}
