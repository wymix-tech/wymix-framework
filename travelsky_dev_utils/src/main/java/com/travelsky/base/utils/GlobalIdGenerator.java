package com.travelsky.base.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.StringJoiner;


public class GlobalIdGenerator {

    private static final Logger log = LoggerFactory.getLogger(GlobalIdGenerator.class);
    /**
     * 默认接受的最大回拨时间，2s
     */
    public static long DEFAULT_TIME_OFFSET = 2000L;
    /**
     * 机器id所占的位数
     */
    private static long workerIdBits = 5L;
    /**
     * 数据标识id所占的位数
     */
    private static long datacenterIdBits = 5L;
    /**
     * 支持的最大机器id，结果是31 (这个移位算法可以很快的计算出几位二进制数所能表示的最大十进制数)
     */
    private static long maxWorkerId = -1L ^ (-1L << workerIdBits);
    /**
     * 支持的最大数据标识id，结果是31
     */
    private static long maxDatacenterId = -1L ^ (-1L << datacenterIdBits);
    ;
    private static GlobalIdGenerator defaultGlobalId = new GlobalIdGenerator(
            GlobalIdGenerator.getMaxWorkerId(GlobalIdGenerator.getDatacenterIdByIp()),
            GlobalIdGenerator.getDatacenterIdByIp());
    ;
    /**
     * 允许的时钟回拨毫秒数
     */
    private final long timeOffset;
    /**
     * 开始时间截 (2015-01-01)
     */
    private long twepoch = 1420041600000L;
    /**
     * 序列在id中占的位数
     */
    private long sequenceBits = 12L;
    /**
     * 机器ID向左移12位
     */
    private long workerIdShift = sequenceBits;
    /**
     * 数据标识id向左移17位(12+5)
     */
    private long datacenterIdShift;
    /**
     * 时间截向左移22位(5+5+12)
     */
    private long timestampLeftShift;
    /**
     * 生成序列的掩码，这里为4095 (0b111111111111=0xfff=4095)
     */
    private long sequenceMask = -1L ^ (-1L << sequenceBits);
    /**
     * 工作机器ID(0~31)
     */
    private long workerId;
    /**
     * 数据中心ID(0~31)
     */
    private long datacenterId;

    /**
     * 毫秒内序列(0~4095)
     */
    private long sequence = 0L;

    /**
     * 上次生成ID的时间截
     */
    private long lastTimestamp = -1L;

    public GlobalIdGenerator() {
        this.workerId = 0;
        this.datacenterId = 0;
        this.timeOffset = DEFAULT_TIME_OFFSET;
        this.build();
    }

    public GlobalIdGenerator(long workerId, long datacenterId) {
        this.build();

        if (workerId > maxWorkerId || workerId < 0) {
            throw new IllegalArgumentException(String
                    .format("worker Id can't be greater than %d or less than 0", maxWorkerId));
        }
        if (datacenterId > maxDatacenterId || datacenterId < 0) {
            throw new IllegalArgumentException(String.format(
                    "datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
        this.timeOffset = DEFAULT_TIME_OFFSET;
    }

    public static long getDatacenterIdByIp(String ipAddr) {
        byte[] mac = (ipAddr + UUID.longUuid(UUID.randomUUID())).getBytes();
        long id = ((0x000000FF & (long) mac[mac.length - 2])
                | (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
        id = id % (maxDatacenterId + 1);
        return id;
    }

    public static long getDatacenterIdByIp() {
        return getDatacenterIdByIp(getIpAddress());
    }

    public static String getIpAddress() {
        String ipAddr = "0.0.0.0";
        try {
            InetAddress ip = InetAddress.getLocalHost();
            ipAddr = ip.getHostAddress();
        } catch (Exception ex) {
            log.warn("获取ip错误");
        }
        return ipAddr;
    }

    public static long getMaxWorkerId(long datacenterId) {
        StringBuilder mpid = new StringBuilder();
        mpid.append(datacenterId);
        String mac = getMac();
        mpid.append(StringUtils.isEmpty(mac) ? UUID.longUuid(UUID.randomUUID()) : mac);
        mpid.append(UUID.longUuid(UUID.randomUUID()));
        /*
         * MAC 或者随机值 的 hashcode 获取16个低位
         */
        return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
    }

    public static String getMac() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            if (network == null) {
                return "";
            } else {
                byte[] mac = network.getHardwareAddress();
                if (null != mac) {
                    StringJoiner j = new StringJoiner("-");
                    for (int i = 0; i < mac.length; i++) {
                        j.add(String.format("%02X", mac[i]));
                    }
                    return j.toString();
                }
            }
        } catch (Exception e) {
            log.warn(" get Mac: " + e.getMessage());
        }
        return "";
    }

    public static GlobalIdGenerator getDefaultGlobalId() {
        return defaultGlobalId;
    }

    public static GlobalIdGenerator initialDefault(long datacenterId, long workerId) {
        defaultGlobalId = new GlobalIdGenerator(workerId, datacenterId);
        return defaultGlobalId;
    }

    public static long generateNextId() {
        return defaultGlobalId.nextId();
    }

    public static String generateNextIdStr() {
        return String.valueOf(defaultGlobalId.nextId());
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getDatacenterId() {
        return datacenterId;
    }

    private void build() {
        this.datacenterIdShift = sequenceBits + workerIdBits;
        this.timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过这个时候应当抛出异常
        if (timestamp < lastTimestamp) {
            if (this.lastTimestamp - timestamp < timeOffset) {
                // 容忍指定的回拨，避免NTP校时造成的异常
                timestamp = lastTimestamp;
            } else {
                throw new RuntimeException(String.format(
                        "Clock moved backwards.  Refusing to generate id for %d milliseconds",
                        lastTimestamp - timestamp));
            }
        }

        // 如果是同一时间生成的，则进行毫秒内序列
        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & sequenceMask;
            // 毫秒内序列溢出
            if (sequence == 0) {
                // 阻塞到下一个毫秒,获得新的时间戳
                timestamp = tilNextMillis(lastTimestamp);
            }
        }
        // 时间戳改变，毫秒内序列重置
        else {
            sequence = 0L;
        }

        // 上次生成ID的时间截
        lastTimestamp = timestamp;

        // 移位并通过或运算拼到一起组成64位的ID
        return ((timestamp - twepoch) << timestampLeftShift) //
                | (datacenterId << datacenterIdShift) //
                | (workerId << workerIdShift) //
                | sequence;
    }

    protected long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    protected long timeGen() {
        return CurrentTimestamp.MILLISSECOND.getTimestamp();
    }

}
