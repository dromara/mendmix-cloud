package com.jeesuite.common.guid;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

public class GUID {

	private static final String LINE_THROUGH = "-";
	
	private static TimestampGUIDGenarator genarator = new TimestampGUIDGenarator(9999);
	private static SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
	
	public static String uuid(){
		String str = StringUtils.replace(UUID.randomUUID().toString(), LINE_THROUGH, StringUtils.EMPTY);
		return str;
	}
	
	public static String guidWithTimestamp(String...prefixs){
		return genarator.next(prefixs);
	}
	
	public static long guid(){
		return snowflakeGenerator.nextId();
	}
	
	
}
