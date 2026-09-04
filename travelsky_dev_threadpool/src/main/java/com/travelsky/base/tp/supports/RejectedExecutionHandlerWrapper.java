package com.travelsky.base.tp.supports;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.LongAdder;

public class RejectedExecutionHandlerWrapper implements RejectedExecutionHandler {

    public final RejectedExecutionHandler rejectedExecutionHandler;
    public final LongAdder rejectedCount;

    public RejectedExecutionHandlerWrapper(RejectedExecutionHandler rejectedExecutionHandler, LongAdder rejectedCount) {
        this.rejectedExecutionHandler = rejectedExecutionHandler;
        this.rejectedCount = rejectedCount;
    }

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        this.rejectedCount.increment();
        this.rejectedExecutionHandler.rejectedExecution(r, executor);
    }
}
