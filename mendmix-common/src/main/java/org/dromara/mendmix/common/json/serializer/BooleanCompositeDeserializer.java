/**
 * 
 */
package org.dromara.mendmix.common.json.serializer;

import java.io.IOException;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * <br>
 * 
 * @author vakinge
 * @date 2023年10月20日
 */
public class BooleanCompositeDeserializer extends JsonDeserializer<Boolean> {

	public final static BooleanCompositeDeserializer instance = new BooleanCompositeDeserializer();
	public static final String FALSE_FLAG = "0";
	public static final String TRUE_FLAG = "1";

	@Override
	public Boolean deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
			throws IOException, JacksonException {
		String value = jsonParser.getText();
		if (TRUE_FLAG.equals(value) || Boolean.parseBoolean(value)) {
			return true;
		}
		return false;
	}
}
