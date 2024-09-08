package org.dromara.mendmix.common.json.serializer;

import java.io.IOException;
import java.util.Objects;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class BigNumberToStringSerializer  extends JsonSerializer<Number> {

	public final static BigNumberToStringSerializer instance = new BigNumberToStringSerializer();
	private static Long bigNumberThreshold = 999999999L;
	@Override
	public void serialize(Number value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if(value instanceof Long) {
			if(((Long)value).compareTo(bigNumberThreshold) > 0) {
				gen.writeString(Objects.toString(value, "0"));
			}else {
				gen.writeNumber((Long)value);
			}
		}else {
			gen.writeString(Objects.toString(value, "0"));
		}
	}

}
