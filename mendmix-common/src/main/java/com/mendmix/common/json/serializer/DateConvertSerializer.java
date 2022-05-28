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
package com.mendmix.common.json.serializer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


public class DateConvertSerializer extends JsonSerializer<Date> {

	private static final String pattern = "yyyy-MM-dd";

	@Override
	public void serialize(Date date, JsonGenerator jgen, SerializerProvider provider) throws JsonProcessingException {
		try {
			DateFormat dateFormat = new SimpleDateFormat(pattern);
			jgen.writeString(dateFormat.format(date));
		} catch (IOException e) {
			throw new RuntimeException("Date转换json异常，格式：" + pattern);
		}
//		log.debug("日期类型序列化");
	}

}
