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
package org.dromara.mendmix.common.json;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.util.ResourceUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakinge</a>
 * @date Sep 1, 2024
 */
public class CompsiteDateFormat  extends DateFormat {


	private static final long serialVersionUID = 1L;
	
	private static CompsiteDateFormat instance = new CompsiteDateFormat();
	
	private static final String STD_DATATIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
	private static final String STD_DATATIME_MS_FORMAT = "yyyy-MM-dd HH:mm:ss SSS";
	private static final String CUSTOM_DATATIME_FORMAT = ResourceUtils.getProperty("mendmix-cloud.jackson.dateFormatPattern");
	
	private static Pattern timeMillsSuffixPattern = Pattern.compile(".*\\s+[0-9]{3}");
	
	private static String[] searchList = new String[] {"T","Z"};
	private static String[] replacementList = new String[] {" "," "};
	
	private CompsiteDateFormat() {
		String timeZoneId = ResourceUtils.getAnyProperty("spring.jackson.time-zone","spring.jackson.timeZone");
		if(timeZoneId != null) {
			setCalendar(Calendar.getInstance(TimeZone.getTimeZone(timeZoneId)));
		}
	}

	public static CompsiteDateFormat singleton() {
		return instance;
	}
	
	private SimpleDateFormat adaptSimpleDateFormat(String dateString) {
		if(StringUtils.isBlank(dateString)) {
			return new SimpleDateFormat(StringUtils.defaultString(CUSTOM_DATATIME_FORMAT, STD_DATATIME_FORMAT));
		}
		if(timeMillsSuffixPattern.matcher(dateString).matches()) {
			return new SimpleDateFormat(STD_DATATIME_MS_FORMAT);
		}
		return new SimpleDateFormat(STD_DATATIME_FORMAT);
	}

	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
		SimpleDateFormat dateFormat = adaptSimpleDateFormat(null);
		return dateFormat.format(date, toAppendTo, fieldPosition);
	}

	@Override
	public Date parse(String source, ParsePosition pos) {
		source = StringUtils.replaceEach(source, searchList, replacementList).trim();
		SimpleDateFormat dateFormat = adaptSimpleDateFormat(source);
		return dateFormat.parse(source, pos);
	}
	
	@Override
	public Date parse(String source) throws ParseException {
		source = StringUtils.replaceEach(source, searchList, replacementList).trim();
		SimpleDateFormat dateFormat = adaptSimpleDateFormat(source);
		return dateFormat.parse(source);
	}
	
	
	 public String adaptFormat(Date date) {
		 return adaptSimpleDateFormat(null).format(date);
	 }

	@Override
	public Object clone() {
		return instance;
	}
}
