package com.jeesuite.kafka.serializer;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.jeesuite.kafka.message.DefaultMessage;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年3月11日
 */
public class MessageJsonDeserializer extends JsonDeserializer<DefaultMessage> {

	private static final String ATTR_CONSUMER_ACK_REQUIRED = "consumerAckRequired";
	private static final String ATTR_BODY = "body";
	private static final String ATTR_MSG_ID = "msgId";
	private static final String ATTR_HEADER = "headers";

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public DefaultMessage deserialize(JsonParser jp, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		JsonNode node = jp.getCodec().readTree(jp);  
        String msgId = node.get(ATTR_MSG_ID).asText();  
        String body = node.get(ATTR_BODY).asText();  
        if(StringUtils.isBlank(body)){
        	body = node.get(ATTR_BODY).toString();
        }
        boolean consumerAckRequired = node.get(ATTR_CONSUMER_ACK_REQUIRED).asBoolean();
		DefaultMessage message = new DefaultMessage(msgId, body).consumerAckRequired(consumerAckRequired);
        if(node.get(ATTR_HEADER) != null){
			Map headers = node.get(ATTR_HEADER).traverse(jp.getCodec()).readValueAs(Map.class);
        	message.setHeaders(headers);
        }
		return message;
	}

}
