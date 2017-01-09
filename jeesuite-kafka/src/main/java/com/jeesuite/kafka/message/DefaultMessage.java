package com.jeesuite.kafka.message;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.common.utils.Utils;

/**
 *  默认消息实体
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年4月19日
 */
public class DefaultMessage implements Serializable {

	private static final long serialVersionUID = 1L;
	private String msgId = UUID.randomUUID().toString();
	private Serializable partitionFactor;//分区因子
	private Map<String, Object> headers;
	
	private transient long partitionHash;

	private Serializable body;
	
	private boolean ackRequired = false;//是否需要消费回执
	
	//兼容一些历史的consumer
	private transient boolean sendBodyOnly = false;
	
	public DefaultMessage() {}

	public DefaultMessage(Serializable body) {
		super();
		this.body = body;
	}
	
	/**
	 * @param body 消息体
	 * @param ackRequired 是否需要消费回执
	 */
	public DefaultMessage(Serializable body, boolean ackRequired) {
		super();
		this.body = body;
		this.ackRequired = ackRequired;
	}
	
	public DefaultMessage(Serializable body, Serializable partitionFactor) {
		super();
		this.body = body;
		this.partitionFactor = partitionFactor;
	}

	public DefaultMessage(Serializable body, long partitionHash, Serializable partitionFactor, boolean ackRequired) {
		super();
		this.body = body;
		this.partitionHash = partitionHash;
		this.partitionFactor = partitionFactor;
		this.ackRequired = ackRequired;
	}

	public String getMsgId() {
		return msgId;
	}

	public Serializable getBody() {
		return body;
	}

	public DefaultMessage body(Serializable body) {
		this.body = body;
		return this;
	}

	public Serializable getPartitionFactor() {
		return partitionFactor;
	}

	public DefaultMessage partitionFactor(Serializable partitionFactor) {
		this.partitionFactor = partitionFactor;
		return this;
	}


	public Map<String, Object> getHeaders() {
		return headers;
	}

	public DefaultMessage header(String key,Object value) {
		if(this.headers == null)this.headers = new HashMap<>();
		this.headers.put(key, value);
		return this;
	}
	
	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public boolean isAckRequired() {
		return ackRequired;
	}
	
	public DefaultMessage ackRequired(boolean ackRequired) {
		this.ackRequired = ackRequired;
		return this;
	}

	public long getPartitionHash() {
		if(partitionHash <= 0 && partitionFactor != null){
			partitionHash = Utils.murmur2(partitionFactor.toString().getBytes());
		}
		return partitionHash;
	}

	public void setPartitionFactor(Serializable partitionFactor) {
		this.partitionFactor = partitionFactor;
	}

	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}

	public DefaultMessage partitionHash(long partitionHash) {
		this.partitionHash = partitionHash;
		return this;
	}

	public void setBody(Serializable body) {
		this.body = body;
	}

	public void setAckRequired(boolean ackRequired) {
		this.ackRequired = ackRequired;
	}

	public boolean isSendBodyOnly() {
		return sendBodyOnly;
	}

	public DefaultMessage sendBodyOnly(boolean sendBodyOnly) {
		this.sendBodyOnly = sendBodyOnly;
		return this;
	}
	
    
}
