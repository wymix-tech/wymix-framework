package org.wymix.framework.monitor.configuration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/** Monitoring never grants anonymous access to management endpoints. */
@AutoConfiguration
@ConditionalOnProperty(prefix = "wymix.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MonitorConfiguration {
}
