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
	
	private int priority = 1;//优先级 1-9  数字越大优先级 越高
	
	private boolean consumerAck = false;//是否需要消费回执
	
	public DefaultMessage() {}

	public DefaultMessage(Serializable body) {
		super();
		this.body = body;
	}
	
	/**
	 * @param body 消息体
	 * @param consumerAck 是否需要消费回执
	 */
	public DefaultMessage(Serializable body, boolean consumerAck) {
		super();
		this.body = body;
		this.consumerAck = consumerAck;
	}

	public DefaultMessage(Serializable body, int priority) {
		this(body, null, 1);
	}
	
	public DefaultMessage(Serializable body, Serializable partitionFactor) {
		this(body, partitionFactor, 1);
	}
	
	public DefaultMessage(Serializable body, Serializable partitionFactor, int priority) {
		super();
		this.body = body;
		this.partitionFactor = partitionFactor;
		this.priority = priority;
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

	public int getPriority() {
		return priority;
	}


	public void setPriority(int priority) {
		this.priority = priority;
	}
	
	public DefaultMessage priority(int priority) {
		this.priority = priority;
		return this;
	}

	public boolean isConsumerAck() {
		return consumerAck;
	}
	
	public DefaultMessage consumerAck(boolean consumerAck) {
		this.consumerAck = consumerAck;
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

	public void setPartitionHash(long partitionHash) {
		this.partitionHash = partitionHash;
	}

	public void setBody(Serializable body) {
		this.body = body;
	}

	public void setConsumerAck(boolean consumerAck) {
		this.consumerAck = consumerAck;
	}
	
	

}
