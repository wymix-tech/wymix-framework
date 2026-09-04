package com.travelsky.base.tp.web;


import com.travelsky.base.tp.supports.ThreadPoolContext;
import com.travelsky.base.tp.supports.ThreadPoolMonitor;
import com.travelsky.base.tp.supports.ThreadPoolOperator;

import java.util.concurrent.TimeUnit;

public class ThreadPoolOperateController {

    public boolean changeThreadPoolConfig(String threadName, int corePoolSize, int maxPoolSize, long keepAliveTime, TimeUnit timeUnit) {
        ThreadPoolContext threadPoolContext = ThreadPoolMonitor.getThreadPoolContext(threadName);
        if (((maxPoolSize == -1) ? threadPoolContext.getMaxPoolSize() : maxPoolSize) < ((corePoolSize == -1) ? threadPoolContext.getCorePoolSize() : corePoolSize)) {
            throw new RuntimeException();
        }
        if (corePoolSize > 0) {
            ThreadPoolOperator.changeCorePoolSize(threadName, corePoolSize);
        }
        if (maxPoolSize > 0) {
            ThreadPoolOperator.changeMaxPoolSize(threadName, maxPoolSize);
        }
        if (keepAliveTime > 0) {
            ThreadPoolOperator.changeKeepAliveTimes(threadName, keepAliveTime, timeUnit);
        }

        return true;
    }

}
