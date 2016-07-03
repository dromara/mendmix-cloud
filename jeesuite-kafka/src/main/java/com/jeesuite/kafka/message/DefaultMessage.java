package com.jeesuite.kafka.message;

import java.io.Serializable;
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
	

	public DefaultMessage(Serializable body) {
		super();
		this.body = body;
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


	public void setHeaders(Map<String, Object> headers) {
		this.headers = headers;
	}


	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public long getPartitionHash() {
		if(partitionHash <= 0 && partitionFactor != null){
			partitionHash = Utils.murmur2(partitionFactor.toString().getBytes());
		}
		return partitionHash;
	}

}
