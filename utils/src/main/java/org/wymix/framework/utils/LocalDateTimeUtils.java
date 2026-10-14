package org.wymix.framework.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;

public class LocalDateTimeUtils {

    public static final Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    public static final LocalDateTime toLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }

    public static Duration between(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end);
    }

    public static long betweenInMills(LocalDateTime start, LocalDateTime end) {
        return between(start, end).toMillis();
    }

    public static double betweenInMin(LocalDateTime start, LocalDateTime end) {
        return betweenInMills(start, end) / 1000 / 60.0D;
    }

    public static double betweenInHours(LocalDateTime start, LocalDateTime end) {
        return betweenInMin(start, end) / 60;
    }

    public static double betweenDays(LocalDateTime start, LocalDateTime end) {
        return betweenInHours(start, end) / 24;
    }


    public static String format(LocalDateTime dateTime, String pattern) {
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime parse(String date, String pattern) {
        return LocalDateTime.parse(date, DateTimeFormatter.ofPattern(pattern));
    }

    public static LocalDateTime add(LocalDateTime dateTime, int num, ChronoUnit unit) {
        return dateTime.plus(num, unit);
    }
}
