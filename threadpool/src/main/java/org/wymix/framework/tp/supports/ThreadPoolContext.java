package org.wymix.framework.tp.supports;


import java.io.Serializable;
import java.util.concurrent.TimeUnit;

public class ThreadPoolContext implements Serializable {

    private String threadName;

    private int corePoolSize;

    private int maxPoolSize;

    private long keepAliveTime;

    private TimeUnit timeUnit;

    private long standby;

    private long running;

    private long remaining;

    private long total;

    private boolean finished;

    private double successRate;

    private double executionRate;

    private double processedRate;

    private long cost;

    private double avgCost;

    public ThreadPoolContext() {

    }

    public ThreadPoolContext(String threadName, int corePoolSize, int maxPoolSize, long keepAliveTime,
                             TimeUnit timeUnit, long standby, long running, long remaining, long total,
                             boolean finished, double successRate, double executionRate, double processedRate,
                             long cost, double avgCost) {
        this.threadName = threadName;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.timeUnit = timeUnit;
        this.standby = standby;
        this.running = running;
        this.remaining = remaining;
        this.total = total;
        this.finished = finished;
        this.successRate = successRate;
        this.executionRate = executionRate;
        this.processedRate = processedRate;
        this.cost = cost;
        this.avgCost = avgCost;
    }

    public void refresh() {
        ExecutorWrapper executorWrapper = ThreadPoolHolder.get(this.threadName);
        this.corePoolSize = executorWrapper.getCorePoolSize();
        this.maxPoolSize = executorWrapper.getMaxPoolSize();
        this.keepAliveTime = executorWrapper.getKeepAliveTime();
        this.timeUnit = executorWrapper.getTimeUnit();
        this.standby = executorWrapper.standby();
        this.running = executorWrapper.running();
        this.remaining = executorWrapper.remaining();
        this.total = executorWrapper.total();
        this.finished = executorWrapper.finished();
        this.successRate = executorWrapper.successRate();
        this.executionRate = executorWrapper.executionRate();
        this.processedRate = executorWrapper.processedRate();
        this.cost = executorWrapper.cost();
        this.avgCost = executorWrapper.avgCost();
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public void setCorePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public long getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(long keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public void setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public long getStandby() {
        return standby;
    }

    public void setStandby(long standby) {
        this.standby = standby;
    }

    public long getRunning() {
        return running;
    }

    public void setRunning(long running) {
        this.running = running;
    }

    public long getRemaining() {
        return remaining;
    }

    public void setRemaining(long remaining) {
        this.remaining = remaining;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getExecutionRate() {
        return executionRate;
    }

    public void setExecutionRate(double executionRate) {
        this.executionRate = executionRate;
    }

    public double getProcessedRate() {
        return processedRate;
    }

    public void setProcessedRate(double processedRate) {
        this.processedRate = processedRate;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    public double getAvgCost() {
        return avgCost;
    }

    public void setAvgCost(double avgCost) {
        this.avgCost = avgCost;
    }
}
