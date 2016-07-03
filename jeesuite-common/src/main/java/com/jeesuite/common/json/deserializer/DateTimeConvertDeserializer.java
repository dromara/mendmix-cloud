package com.jeesuite.common.json.deserializer;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;


public class DateTimeConvertDeserializer extends JsonDeserializer<Date> {

	private static String pattern = "yyyy-MM-dd HH:mm:ss";

	@Override
	public Date deserialize(JsonParser jsonParser, DeserializationContext dc) throws JsonProcessingException {
		Date date = null;
		DateFormat dateFormat = new SimpleDateFormat(pattern);
		try {
			String val = jsonParser.getText();

			date = dateFormat.parse(val);
		} catch (ParseException | IOException pex) {
			throw new RuntimeException("json转换Date异常，格式：" + pattern);
		}

		return date;
	}

}
