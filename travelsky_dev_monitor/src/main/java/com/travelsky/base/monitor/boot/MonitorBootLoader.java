package com.travelsky.base.monitor.boot;

import com.travelsky.base.core.boot.BootLoader;
import com.travelsky.base.monitor.listener.MonitorApplicationStartedEventListener;
import org.springframework.boot.SpringApplication;

public class MonitorBootLoader {

    public static SpringApplication genApp(Class clazz) {
        BootLoader.addListener(new MonitorApplicationStartedEventListener());
        return BootLoader.genApp(clazz);
    }

}
