package org.wymix.framework.tp.supports;

import java.util.concurrent.atomic.LongAdder;

public class RunnableWrapper implements Runnable {

    private final Runnable runnable;
    private final LongAdder finished;
    private final LongAdder cost;
    private final LongAdder successCount;

    public RunnableWrapper(Runnable runnable, LongAdder finished, LongAdder cost, LongAdder successCount) {
        this.runnable = runnable;
        this.finished = finished;
        this.cost = cost;
        this.successCount = successCount;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        try {
            this.runnable.run();
            this.successCount.increment();
        } finally {
            cost.add(System.currentTimeMillis() - start);
            finished.increment();
        }

    }
}

