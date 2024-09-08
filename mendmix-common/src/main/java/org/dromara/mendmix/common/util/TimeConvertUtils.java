package org.dromara.mendmix.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年7月26日
 */
public class TimeConvertUtils {
	
	private static final String UTC_TIME_ZONE = "UTC+0";
	private static final String DATA_TIME_PATTERN = "uuuu-MM-dd HH:mm:ss";
	public static final String localTimeZoneId = ZoneId.systemDefault().getId();
	public static final int TIME_ZONE_OFFSET = ZonedDateTime.now().getOffset().getTotalSeconds()/60;
	public static final boolean CURRENT_TIME_ZONE_IS_UTC = TIME_ZONE_OFFSET == 0;
	
	public static String timeZoneConvert(String timeStr,String fromTimeZone,String toTimeZone) {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATA_TIME_PATTERN);
    	ZoneId localZoneId = ZoneId.of(fromTimeZone);
    	LocalDateTime localtDateTime = LocalDateTime.parse(timeStr, formatter);
    	ZonedDateTime localZonedDateTime = ZonedDateTime.of(localtDateTime, localZoneId );
    	ZonedDateTime utcZonedDateTime = localZonedDateTime.withZoneSameInstant(ZoneId.of(toTimeZone));
    	String formatValue = utcZonedDateTime.format(formatter);
		return formatValue;
    }
	
	public static String timeZoneConvert(Date date,String fromTimeZone,String toTimeZone) {
		return timeZoneConvert(DateUtils.format(date), fromTimeZone, toTimeZone);
    }
    
    public static String getCurrentTimeAsFormatUTC() {
    	DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATA_TIME_PATTERN);
        ZonedDateTime localZonedDateTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
    	ZonedDateTime utcZonedDateTime = localZonedDateTime.withZoneSameInstant(ZoneOffset.UTC);
    	return utcZonedDateTime.format(formatter);
    }
    
    public static Date getCurrentTimeAsUTC() {
    	return DateUtils.parseDate(getCurrentTimeAsFormatUTC());
    }
    
    public static Date toUTC(Date date,String timeZone) {
    	return DateUtils.parseDate(timeZoneConvert(date,timeZone,UTC_TIME_ZONE));
    }
     
    public static String toUTC(String timeStr,String timeZone) {
    	return timeZoneConvert(timeStr,timeZone,UTC_TIME_ZONE);
    }
    
    public static Date toUTC(Date date) {
    	return DateUtils.parseDate(timeZoneConvert(date,localTimeZoneId,UTC_TIME_ZONE));
    }
     
    public static String toUTC(String timeStr) {
    	return timeZoneConvert(timeStr,localTimeZoneId,UTC_TIME_ZONE);
    }
    
    public static Date reverseFromUTC(Date date,String timeZone) {
    	return DateUtils.parseDate(timeZoneConvert(date,UTC_TIME_ZONE,timeZone));
    }
     
    public static String reverseFromUTC(String timeStr,String timeZone) {
    	return timeZoneConvert(timeStr,UTC_TIME_ZONE,timeZone);
    }
    
    public static Date reverseFromUTC(Date date) {
    	return DateUtils.parseDate(timeZoneConvert(date,UTC_TIME_ZONE,localTimeZoneId));
    }
     
    public static String reverseFromUTC(String timeStr) {
    	return timeZoneConvert(timeStr,UTC_TIME_ZONE,localTimeZoneId);
    }
    
    public static boolean isSystemDefaultUTC() {
		return CURRENT_TIME_ZONE_IS_UTC;
	}
    
    public static Date[] toUTCDateRegion(Date date,String timeZone) {
    	Date[] res = new Date[2];
    	res[0] = toUTC(DateUtils.getDayBegin(date), timeZone);
    	res[1] = toUTC(DateUtils.getDayEnd(date), timeZone);
    	return res;
    }

}
