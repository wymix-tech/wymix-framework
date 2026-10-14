package org.wymix.framework.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class DateUtils {

    public static final String YYYYMMDD = "yyyyMMdd";

    public static final String YYYYMMDDHHMMSS_LINE = "yyyy-MM-dd HH:mm:ss";

    public static final String YYYYMMDD_LINE = "yyyy-MM-dd";

    public static final String[] NORMAL_PATTERNS = {YYYYMMDD_LINE, YYYYMMDD, YYYYMMDDHHMMSS_LINE};

    private static final ThreadLocal<ConcurrentMap<String, DateFormat>> DATE_FORMAT = ThreadLocal.withInitial(ConcurrentHashMap::new);

    public static final String format(Date date, String pattern) {
        DateFormat format = getFormat(pattern);
        return format.format(date);
    }


    private static final DateFormat getFormat(String pattern) {
        ConcurrentMap<String, DateFormat> map = DATE_FORMAT.get();
        if (map == null) {
            map = new ConcurrentHashMap<>();
            DATE_FORMAT.set(map);
        }
        if (!map.containsKey(pattern)) {
            map.put(pattern, new SimpleDateFormat(pattern));
        }
        return map.get(pattern);
    }

    public static final Date parse(String dateStr, String pattern) {
        if (StringUtils.isEmpty(pattern)) {
            pattern = YYYYMMDDHHMMSS_LINE;
        }
        if (StringUtils.isEmpty(dateStr)) {
            return null;
        }
        Date ret = null;
        DateFormat df = getFormat(pattern);
        try {
            ret = df.parse(dateStr);
        } catch (Exception e) {

        }
        return ret;
    }

    public static final Date parse(String dateStr) {
        return parse(dateStr, YYYYMMDDHHMMSS_LINE);
    }

    public static Locale getDefaultLocale() {
        if (TimeZone.getDefault().getRawOffset() == (TimeZone.getTimeZone("GMT+08").getRawOffset())) {
            return Locale.CHINA;
        }
        return Locale.getDefault();
    }


    public static long betweenInMills(Date start, Date end) {
        return (end.getTime() - start.getTime());
    }

    public static double betweenInMin(Date start, Date end) {
        return betweenInMills(start, end) / 1000 / 60.0D;
    }

    public static double betweenInHours(Date start, Date end) {
        return betweenInMin(start, end) / 60;
    }

    public static double betweenInDays(Date start, Date end) {
        return betweenInHours(start, end) / 24;
    }

    public static Date getStartOfDate(Date date) {
        return parse(format(date, "yyyyMMdd"), "yyyyMMdd");
    }

    public static Date getEndOfDate(Date date) {
        return parse(format(date, "yyyyMMdd") + " 23:59:59.999", "yyyyMMdd HH:mm:ss.SSS");
    }

}
