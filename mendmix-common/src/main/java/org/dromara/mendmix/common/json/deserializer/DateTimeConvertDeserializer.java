/*
 * Copyright 2016-2020 www.jeesuite.com.
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
package org.dromara.mendmix.common.json.deserializer;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.dromara.mendmix.common.json.CompsiteDateFormat;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;


public class DateTimeConvertDeserializer extends JsonDeserializer<Date> {

	@Override
	public Date deserialize(JsonParser jsonParser, DeserializationContext dc) throws JsonProcessingException {
		String val = null;
		try {
			val = jsonParser.getText();
			return CompsiteDateFormat.singleton().parse(val);
		} catch (ParseException | IOException pex) {
			throw new RuntimeException("json转换Date异常["+val+"]");
		}
	}

}
