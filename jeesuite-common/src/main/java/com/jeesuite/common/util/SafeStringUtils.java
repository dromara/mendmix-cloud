package com.jeesuite.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.GlobalConstants;

public class SafeStringUtils {

	private static List<String> sensitiveKeys = new ArrayList<>(
			Arrays.asList("password", "key", "secret", "token", "credentials"));

	public static String hideSensitiveKeyValue(String key, String value) {
		if (StringUtils.isAnyBlank(key, value))
			return "";
		if (key.endsWith(".enabled"))
			return value;
		boolean is = false;
		for (String k : sensitiveKeys) {
			if (is = key.toLowerCase().contains(k))
				break;
		}
		return is ? getDesensitizedValue(value) : value;
	}
	
	public static String getDesensitizedValue(String value) {
		int length = value.length();
		if (length <= 2) {
			return value;
		}
		int partLen = length / 3;
		StringBuilder builder = new StringBuilder();
		builder.append(value.substring(0, partLen));
		int asteriskNums = partLen;
		if(partLen <= 3 && length > partLen * 3) {
			asteriskNums = partLen + 1;
		}
		for (int i = 0; i < asteriskNums; i++) {
			builder.append(GlobalConstants.ASTERISK);	
		}
		builder.append(value.substring(partLen + asteriskNums));
			
		return builder.toString();
	}
	
}
