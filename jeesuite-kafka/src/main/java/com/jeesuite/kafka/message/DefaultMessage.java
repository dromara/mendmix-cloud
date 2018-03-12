package com.jeesuite.kafka.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.utils.Utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.kafka.serializer.MessageJsonDeserializer;

/**
 *  默认消息实体
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年4月19日
 */
@JsonDeserialize(using = MessageJsonDeserializer.class) 
@JsonInclude(Include.NON_NULL)
public class DefaultMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	private String msgId;
	private Serializable body;
	private Map<String, Object> headers;
	private boolean consumerAckRequired = false;//是否需要消费回执
	
	private transient long partitionHash;
	//兼容一些历史的consumer
	private transient boolean sendBodyOnly = false;
	private transient String topic;
	private transient int partition;
	private transient long offset;
	
	public DefaultMessage() {}


	public DefaultMessage(Serializable body) {
		this(TokenGenerator.generate(), body);
	}
	
	public DefaultMessage(String msgId, Serializable body) {
		this.msgId = StringUtils.isBlank(msgId) ? TokenGenerator.generate() : msgId;
		this.body = body;
	}

	public String getMsgId() {
		if(StringUtils.isBlank(msgId)){
			msgId =  TokenGenerator.generate();
		}
		return msgId;
	}
	
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public Serializable getBody() {
		return body;
	}
	
	public void setBody(Serializable body) {
		this.body = body;
	}

	public DefaultMessage body(Serializable body) {
		this.body = body;
		return this;
	}
	
	public Map<String, Object> getHeaders() {
		return headers;
	}
	
	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	public DefaultMessage header(String key,Object value) {
		if(this.headers == null)this.headers = new HashMap<>();
		this.headers.put(key, value);
		return this;
	}
	
	public DefaultMessage consumerAckRequired(boolean consumerAckRequired) {
		this.consumerAckRequired = consumerAckRequired;
		return this;
	}
	
	public boolean isConsumerAckRequired() {
		return consumerAckRequired;
	}

	public void setConsumerAckRequired(boolean consumerAckRequired) {
		this.consumerAckRequired = consumerAckRequired;
	}

	public DefaultMessage partitionFactor(Serializable partitionFactor) {
		if(partitionFactor != null){
			partitionHash = Utils.murmur2(partitionFactor.toString().getBytes());
		}
		return this;
	}

	public long partitionHash() {
		return partitionHash;
	}

	public boolean sendBodyOnly() {
		return sendBodyOnly;
	}

	public DefaultMessage sendBodyOnly(boolean sendBodyOnly) {
		this.sendBodyOnly = sendBodyOnly;
		return this;
	}

	public void setTopicMetadata(String topic,int partition,long offset) {
		this.topic = topic;
	}
	
	public String topic() {
		return topic;
	}

	public int partition() {
		return partition;
	}

	public long offset() {
		return offset;
	}
	
}
