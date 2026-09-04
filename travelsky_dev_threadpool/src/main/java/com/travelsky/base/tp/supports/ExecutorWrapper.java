package com.travelsky.base.tp.supports;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public interface ExecutorWrapper extends ExecutorService {

    int getCorePoolSize();

    void setCorePoolSize(int corePoolSize);

    int getMaxPoolSize();

    void setMaxPoolSize(int maxPoolSize);

    long getKeepAliveTime();

    TimeUnit getTimeUnit();

    void setKeepAliveTime(long keepAliveTime, TimeUnit timeUnit);

    long remaining();

    long standby();

    long running();

    long total();

    long cost();

    double avgCost();

    boolean finished();

    double successRate();

    double executionRate();

    double processedRate();
}

