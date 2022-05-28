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
package com.mendmix.springweb.utils;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class UnsafeCharCheckUtils {
	
	private static final String[] SEARCH_LIST = new String[]{"'","--","<",">","%3C","%3E","%3c","%3e","&lt;","&gt;"};
	private static final String[] REPLACEMENT_LIST = new String[]{"‘",StringUtils.EMPTY,StringUtils.EMPTY,StringUtils.EMPTY,StringUtils.EMPTY,StringUtils.EMPTY,StringUtils.EMPTY,StringUtils.EMPTY,StringUtils.EMPTY,StringUtils.EMPTY};
	
	public static enum CheckMode{
		REPLACE,INTERCEPT
	}

	private static Pattern openclosePattern = Pattern.compile("<.*>(.*?)</.*>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern scriptPattern = Pattern.compile("<script>(.*?)</script>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern imgPattern = Pattern.compile("<img(.*?)>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern aPattern = Pattern.compile("<a>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern evalPattern = Pattern.compile("eval\\((.*?)\\)",Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern e­xpressionPattern = Pattern.compile("e­xpression\\((.*?)\\)",Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);
	private static Pattern onEventPattern = Pattern.compile("on(\\w{4,9})\\s+=",Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

	public static boolean isSafeString(String value,boolean isQueryPram){
		if(StringUtils.isBlank(value))return true;
		if(isQueryPram){
			for (String str : SEARCH_LIST) {
				if(value.contains(str))return false;
			}
			return true;
		}
		return openclosePattern.matcher(value).find() == false 
				&& imgPattern.matcher(value).find() == false
				&& aPattern.matcher(value).find() == false
				&& evalPattern.matcher(value).find() == false
				&& e­xpressionPattern.matcher(value).find() == false
				&& onEventPattern.matcher(value).find() == false;
	}
	
	public static String replaceSpecChars(String value) {
		if (StringUtils.isNotBlank(value)) {
			// Avoid null characters
			value = value.replaceAll("\0", StringUtils.EMPTY);
			value = StringUtils.replaceEach(value, SEARCH_LIST, REPLACEMENT_LIST);
			// Avoid anything between script tags
			//value = scriptPattern.matcher(value).replaceAll(StringUtils.EMPTY);
			//value = imgPattern.matcher(value).replaceAll(StringUtils.EMPTY);
			//value = aPattern.matcher(value).replaceAll(StringUtils.EMPTY);
			// Avoid eval(...) e­xpressions
			value = evalPattern.matcher(value).replaceAll(StringUtils.EMPTY);
			// Avoid e­xpression(...) e­xpressions
			value = e­xpressionPattern.matcher(value).replaceAll(StringUtils.EMPTY);
			value = onEventPattern.matcher(value).replaceAll(StringUtils.EMPTY);
		}
		return value;
	}
	
	public static void main(String[] args) {
		String str = "aaa@aa.com\"&gt;&lt;script&gt;alert(document.cookie)&lt;/script&gt;";
		System.out.println(isSafeString(str,true));
		System.out.println(replaceSpecChars(str));

	}
}
