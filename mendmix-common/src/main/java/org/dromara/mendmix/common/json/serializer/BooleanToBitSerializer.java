package org.dromara.mendmix.common.json.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class BooleanToBitSerializer extends JsonSerializer<Boolean> {

	public final static BooleanToBitSerializer instance = new BooleanToBitSerializer();

	@Override
	public void serialize(Boolean value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		if (value == null) {
			gen.writeString(BooleanCompositeDeserializer.FALSE_FLAG);
		} else {
			gen.writeString(value ? BooleanCompositeDeserializer.TRUE_FLAG : BooleanCompositeDeserializer.FALSE_FLAG);
		}
	}

}
