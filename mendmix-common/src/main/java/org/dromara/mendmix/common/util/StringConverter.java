/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.common.GlobalConstants;

public class StringConverter {

	private static List<String> sensitiveKeys = new ArrayList<>(
			Arrays.asList("password", "key", "secret", "token", "credentials"));

	private static String[] paddingzeros = new String[] { "", "0", "00", "000", "0000", "00000", "000000", "0000000",
			"00000000", "000000000" };

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
		if (partLen <= 3 && length > partLen * 3) {
			asteriskNums = partLen + 1;
		}
		for (int i = 0; i < asteriskNums; i++) {
			builder.append(GlobalConstants.ASTERISK);
		}
		builder.append(value.substring(partLen + asteriskNums));

		return builder.toString();
	}

	public static String toCamelCase(String underlineStr) {
		if (underlineStr == null) {
			return null;
		}
		if (!underlineStr.contains(GlobalConstants.UNDER_LINE)) {
			return underlineStr;
		}
		// 分成数组
		char[] charArray = underlineStr.toCharArray();
		// 判断上次循环的字符是否是"_"
		boolean underlineBefore = false;
		StringBuffer buffer = new StringBuffer();
		for (int i = 0, l = charArray.length; i < l; i++) {
			// 判断当前字符是否是"_",如果跳出本次循环
			if (charArray[i] == 95) {
				underlineBefore = true;
			} else if (underlineBefore) {
				// 如果为true，代表上次的字符是"_",当前字符需要转成大写
				buffer.append(charArray[i] -= 32);
				underlineBefore = false;
			} else {
				// 不是"_"后的字符就直接追加
				buffer.append(charArray[i]);
			}
		}
		return buffer.toString();
	}

	public static String toUnderlineCase(String camelCaseStr) {
		if (camelCaseStr == null) {
			return null;
		}
		// 将驼峰字符串转换成数组
		char[] charArray = camelCaseStr.toCharArray();
		StringBuffer buffer = new StringBuffer();
		// 处理字符串
		for (int i = 0, l = charArray.length; i < l; i++) {
			if (charArray[i] >= 65 && charArray[i] <= 90) {
				buffer.append(GlobalConstants.UNDER_LINE).append(charArray[i] += 32);
			} else {
				buffer.append(charArray[i]);
			}
		}
		return buffer.toString();
	}

	public static String paddingZeros(String str, int expectLength) {
		// 补0
		int len = expectLength - str.length();
		if (len > 0) {
			str = paddingzeros[len] + str;
		}
		return str;
	}

	public static void main(String[] args) {
		System.out.println(toCamelCase("userName"));
		System.out.println(toCamelCase("user_name"));
		System.out.println(toUnderlineCase("userName"));
	}

}
