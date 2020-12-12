package com.jeesuite.common.json.serializer;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class CurrencyAmountSerializer extends JsonSerializer<BigDecimal> {

	private static final String FORMAT_ZERO = "0.00";

	@Override
	public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeString(value == null ? FORMAT_ZERO : value.setScale(2,RoundingMode.HALF_UP).toPlainString());
	}

}
