/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.common.util;

import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * 日期处理工具类
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2012年12月20日
 */
public class DateUtils extends org.apache.commons.lang3.time.DateUtils{
	
	private static final String TIME_00_00_00 = " 00:00:00";
	private static final String TIME_23_59_59 = " 23:59:59";
	private static final String[] weekDays = { "sunday","monday", "tuesday", "wednesday", "thursday", "friday", "saturday"};
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String TIMESTAMP_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_PATTERN = "HH:mm";
    /**
     * 解析日期<br>
     * 支持格式：<br>
     * generate by: vakin jiang at 2012-3-1
     * 
     * @param dateStr
     * @return
     */
    public static Date parseDate(String dateStr) {
        SimpleDateFormat format = null;
        if (StringUtils.isBlank(dateStr))
            return null;
        String _dateStr = dateStr.trim();
        try {
            if (_dateStr.matches("\\d{1,2}[A-Z]{3}")) {
                _dateStr = _dateStr
                        + (Calendar.getInstance().get(Calendar.YEAR) - 2000);
            }
            // 01OCT12
            if (_dateStr.matches("\\d{1,2}[A-Z]{3}\\d{2}")) {
                format = new SimpleDateFormat("ddMMMyy", Locale.ENGLISH);
            } else if (_dateStr.matches("\\d{1,2}[A-Z]{3}\\d{4}.*")) {// 01OCT2012
                // ,01OCT2012
                // 1224,01OCT2012
                // 12:24
                _dateStr = _dateStr.replaceAll("[^0-9A-Z]", "")
                        .concat("000000").substring(0, 15);
                format = new SimpleDateFormat("ddMMMyyyyHHmmss", Locale.ENGLISH);
            } else {
                StringBuffer sb = new StringBuffer(_dateStr);
                String[] tempArr = _dateStr.split("\\s+");
                tempArr = tempArr[0].split("-|\\/");
                if (tempArr.length == 3) {
                    if (tempArr[1].length() == 1) {
                        sb.insert(5, "0");
                    }
                    if (tempArr[2].length() == 1) {
                        sb.insert(8, "0");
                    }
                }
                _dateStr = sb.append("000000").toString().replaceAll("[^0-9]",
                        "").substring(0, 14);
                if (_dateStr.matches("\\d{14}")) {
                    format = new SimpleDateFormat("yyyyMMddHHmmss");
                }
            }
            Date date = format.parse(_dateStr);
            return date;
        } catch (Exception e) {
            throw new RuntimeException("无法解析日期字符[" + dateStr + "]");
        }
    }
    /**
     * 解析日期字符串转化成日期格式<br>
     * generate by: vakin jiang at 2012-3-1
     * 
     * @param dateStr
     * @param pattern
     * @return
     */
    public static Date parseDate(String dateStr, String pattern) {
        try {
            SimpleDateFormat format = null;
            if (StringUtils.isBlank(dateStr))
                return null;
            if (StringUtils.isNotBlank(pattern)) {
                format = new SimpleDateFormat(pattern);
                return format.parse(dateStr);
            }
            return parseDate(dateStr);
        } catch (Exception e) {
            throw new RuntimeException("无法解析日期字符[" + dateStr + "]");
        }
    }
    /**
     * 获取一天开始时间<br>
     * generate by: vakin jiang at 2011-12-23
     * 
     * @param date
     * @return
     */
    public static Date getDayBegin(Date date) {
        String format = DateFormatUtils.format(date, DATE_PATTERN);
        return parseDate(format.concat(TIME_00_00_00));
    }
    /**
     * 获取一天结束时间<br>
     * generate by: vakin jiang at 2011-12-23
     * 
     * @param date
     * @return
     */
    public static Date getDayEnd(Date date) {
        String format = DateFormatUtils.format(date, DATE_PATTERN);
        return parseDate(format.concat(TIME_23_59_59));
    }
    /**
     * 时间戳格式转换为日期（年月日）格式<br>
     * generate by: vakin jiang at 2011-12-23
     * 
     * @param date
     * @return
     */
    public static Date timestamp2Date(Date date) {
        return formatDate(date, DATE_PATTERN);
    }
    
     /**
     * 格式化日期格式为：ddMMMyy<br>
     * generate by: vakin jiang
     *                    at 2012-10-17
     * @param dateStr
     * @return
     */
    public static String format2ddMMMyy(Date date){
        SimpleDateFormat format = new SimpleDateFormat("ddMMMyy", Locale.ENGLISH);
        return format.format(date).toUpperCase();
    }
    
     /**
     * 格式化日期格式为：ddMMMyy<br>
     * generate by: vakin jiang
     *                    at 2012-10-17
     * @param dateStr
     * @return
     */
    public static String format2ddMMMyy(String dateStr){
        SimpleDateFormat format = new SimpleDateFormat("ddMMMyy", Locale.ENGLISH);
        return format.format(DateUtils.parseDate(dateStr)).toUpperCase();
    }
    /**
     * 格式化日期字符串<br>
     * generate by: vakin jiang at 2012-3-7
     * 
     * @param dateStr
     * @param patterns
     * @return
     */
    public static String formatDateStr(String dateStr, String... patterns) {
        String pattern = TIMESTAMP_PATTERN;
        if (patterns != null && patterns.length > 0
                && StringUtils.isNotBlank(patterns[0])) {
            pattern = patterns[0];
        }
        return DateFormatUtils.format(parseDate(dateStr), pattern);
    }
    /**
     * 格式化日期为日期字符串<br>
     * generate by: vakin jiang at 2012-3-7
     * 
     * @param orig
     * @param patterns
     * @return
     */
    public static String format(Date date, String... patterns) {
        if (date == null)
            return "";
        String pattern = TIMESTAMP_PATTERN;
        if (patterns != null && patterns.length > 0
                && StringUtils.isNotBlank(patterns[0])) {
            pattern = patterns[0];
        }
        return DateFormatUtils.format(date, pattern);
    }
    public static String format2DateStr(Date date) {
        return format(date, DATE_PATTERN);
    }
    /**
     * 格式化日期为指定格式<br>
     * generate by: vakin jiang at 2012-3-7
     * 
     * @param orig
     * @param patterns
     * @return
     */
    public static Date formatDate(Date orig, String... patterns) {
        String pattern = TIMESTAMP_PATTERN;
        if (patterns != null && patterns.length > 0
                && StringUtils.isNotBlank(patterns[0])) {
            pattern = patterns[0];
        }
        return parseDate(DateFormatUtils.format(orig, pattern));
    }
    
    /**
     * 比较两个时间相差多少秒
     * */
    public static long getDiffSeconds(Date d1, Date d2) {
        return Math.abs((d2.getTime() - d1.getTime()) / 1000);
    }
    
    /**
     * 比较两个时间相差多少分钟
     * */
    public static long getDiffMinutes(Date d1, Date d2) {
        long diffSeconds = getDiffSeconds(d1, d2);
        return diffSeconds/60;
    }
    /**
     * 比较两个时间相差多少天
     * */
    public static long getDiffDay(Date d1, Date d2) {
        long between = Math.abs((d2.getTime() - d1.getTime()) / 1000);
        long day = between / 60 / 60 / 24;
        return (long) Math.floor(day);
    }
    /**
     * 返回传入时间月份的最后一天
     * */
    public static Date lastDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int value = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, value);
        return cal.getTime();
    }
    /**
     * 返回传入时间月份的第一天
     * */
    public static Date firstDayOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int value = cal.getActualMinimum(Calendar.DAY_OF_MONTH);
        cal.set(Calendar.DAY_OF_MONTH, value);
        return cal.getTime();
    }
    /**
     * 获取两个时间相差月份
     * */
    public static int getDiffMonth(Date start, Date end) {
    	Calendar startCalendar = Calendar.getInstance();
		startCalendar.setTime(start);
		Calendar endCalendar = Calendar.getInstance();
		endCalendar.setTime(end);
		return (endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR)) * 12
				+ endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
    }
    /**
     * 计算并格式化消耗时间<br>
     * generate by: vakin jiang at 2012-2-16
     * 
     * @param startPoint
     * @return
     */
    public static String formatTimeConsumingInfo(long startPoint) {
        StringBuffer buff = new StringBuffer();
        long totalMilTimes = System.currentTimeMillis() - startPoint;
        int hour = (int) Math.floor(totalMilTimes / (60*60*1000));
        int mi = (int) Math.floor(totalMilTimes / (60*1000));
        int se = (int) Math.floor((totalMilTimes - 60000 * mi) / 1000);
        if(hour > 0)buff.append(hour).append("小时");
        if(mi > 0)buff.append(mi).append("分");
        if(hour == 0)buff.append(se).append("秒");
        return buff.toString();
    }
    /**
     * 判断是否为闰年<br>
     * generate by: zengqw at 2012-9-26
     */
    public static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }
    
    public static Date add(Date date, int calendarField, int amount) {
        if (date == null) {
            throw new IllegalArgumentException("The date must not be null");
        }
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.add(calendarField, amount);
        return c.getTime();
    }
    
    public static String getDateWeekEnName(Date date){
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int w = cal.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0) {
            w = 0;
        }
        return weekDays[w];
    }
    
    
    public static Date firstDayOfWeek(Date date) {
		ZoneId zoneId = ZoneId.systemDefault();
    	long timeMills =  LocalDate.now(Clock.fixed(Instant.ofEpochMilli(date.getTime()),zoneId))
    	.with(DayOfWeek.MONDAY)
    	.atStartOfDay(zoneId)
    	.toInstant()
    	.toEpochMilli();
    	return new Date(timeMills);
     }
    
    public static Date lastDayOfWeek(Date date) {
 		ZoneId zoneId = ZoneId.systemDefault();
 		long timeMills =  LocalDate.now(Clock.fixed(Instant.ofEpochMilli(date.getTime()),zoneId))
     	.with(DayOfWeek.SUNDAY)
     	.atStartOfDay(zoneId)
     	.toInstant()
     	.toEpochMilli();
 		return new Date(timeMills);
      }
}
