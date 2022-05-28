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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.GlobalConstants;

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
