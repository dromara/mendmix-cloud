/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.amqp;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.util.BeanUtils;
import com.mendmix.common.util.HttpUtils;
import com.mendmix.common.util.JsonUtils;

/**
 * 
 * <br>
 * Class Name : MQMessage
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
@SuppressWarnings("unchecked")
@JsonInclude(Include.NON_NULL)
public class MQMessage {

	private String msgId;
	private String topic;
	private String tag;
	private String bizKey;
	private Object body;
	private String checkUrl;
	private String txId;
	private Map<String, String> headers;
	@JsonIgnore
	private Long processTime; //处理时间
	private Long deliverTime; //定时消息
	//=======================
	
	private Integer partition;
	@JsonIgnore
	private long offset;
	private int consumeTimes;
	@JsonIgnore
	private Object originMessage;

	public MQMessage() {}

	public static MQMessage build(String json) {
		MQMessage message = JsonUtils.toObject(json, MQMessage.class);
		if (!BeanUtils.isSimpleDataType(message.body.getClass())) {
			message.setBody(JsonUtils.toJson(message.body));
		}
		return message;
	}

	public MQMessage(String topic, Object body) {
		this(topic, null, body);
	}

	public MQMessage(String topic, String bizKey, Object body) {
		this(topic, null, bizKey, body);
	}

	public MQMessage(String topic, String tag, String bizKey, Object body) {
		this();
		this.topic = MQContext.rebuildWithNamespace(topic);
		this.tag = tag;
		this.bizKey = bizKey;
		if (body instanceof byte[]) {
			this.body = new String((byte[]) body, StandardCharsets.UTF_8);
		} else {
			this.body = body;
		}
	}


	public String getCheckUrl() {
		return checkUrl;
	}

	public void setCheckUrl(String checkUrl) {
		this.checkUrl = checkUrl;
	}

	/**
	 * @return the topic
	 */
	public String getTopic() {
		return topic;
	}

	/**
	 * @param topic
	 *            the topic to set
	 */
	public void setTopic(String topic) {
		this.topic = MQContext.rebuildWithNamespace(topic);
	}


	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	/**
	 * @return the bizKey
	 */
	public String getBizKey() {
		return bizKey;
	}

	/**
	 * @param bizKey
	 *            the bizKey to set
	 */
	public void setBizKey(String bizKey) {
		this.bizKey = bizKey;
	}

	public Long getProcessTime() {
		return processTime;
	}

	public void setProcessTime(Long processTime) {
		this.processTime = processTime;
	}

	/**
	 * @return the body
	 */
	public Object getBody() {
		return body;
	}

	/**
	 * @param body
	 *            the body to set
	 */
	public void setBody(Object body) {
		this.body = body;
	}
	

	public String getTxId() {
		return txId;
	}

	public void setTxId(String txId) {
		this.txId = txId;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public Long getDeliverTime() {
		return deliverTime;
	}

	public void setDeliverTime(Long deliverTime) {
		this.deliverTime = deliverTime;
	}
	
	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	
	public long getOffset() {
		return offset;
	}

	public int getConsumeTimes() {
		return consumeTimes;
	}
	
	public Integer getPartition() {
		return partition;
	}

	public void setPartition(Integer partition) {
		this.partition = partition;
	}

	public <T> T getOriginMessage(Class<?> clazz) {
		return (T) this.originMessage;
	}

	public void setOriginMessage(Object originMessage) {
		this.originMessage = originMessage;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}
	
	public void addHeader(String name,String value) {
		if(headers == null)headers = new LinkedHashMap<>();
		headers.put(name, value);
	}

	public void onProducerFinished(String msgId,int partition,long offset) {
		this.msgId = msgId;
		this.partition = partition;
		this.offset = offset;
	}

	public byte[] bodyAsBytes() {
		if (BeanUtils.isSimpleDataType(body.getClass())) {
			return body.toString().getBytes(StandardCharsets.UTF_8);
		} else {
			return JsonUtils.toJson(body).getBytes(StandardCharsets.UTF_8);
		}
	}

	public String toMessageValue(boolean onlyBody) {
		if(onlyBody) {
			if (BeanUtils.isSimpleDataType(body.getClass())) {
				return body.toString();
			}else {
				return JsonUtils.toJson(body);
			}
		}
		mergeContextHeaders();
		return JsonUtils.toJson(this);
	}

	public void mergeContextHeaders() {
		if(headers == null) {
			headers = CurrentRuntimeContext.getContextHeaders();
		}else {
			headers.putAll(CurrentRuntimeContext.getContextHeaders());
		}
	}

	public <T> T toObject(Class<T> clazz) {
		if(body instanceof String == false) {
			if(body.getClass() == clazz) {
				return (T) body;
			}else {
				return BeanUtils.copy(body, clazz);
			}
		}
		return JsonUtils.toObject(body.toString(), clazz);
	}

	public <T> List<T> toList(Class<T> clazz) {
		if(body instanceof List) {
			return BeanUtils.copy((List)body, clazz);
		}
		return JsonUtils.toList(body.toString(), clazz);
	}

	public boolean originStatusCompleted() {
		if (StringUtils.isAnyBlank(txId, checkUrl))
			return true;
		
		String url = String.format("%s?txId=%s", checkUrl,txId);

		String status = HttpUtils.get(url).getBody();
		if (Boolean.parseBoolean(status)) {
			try {
				Thread.sleep(100);
			} catch (Exception e) {
			}
			status = HttpUtils.get(url).getBody();
		}

		return Boolean.parseBoolean(status);

	}

	public String logString() {
		return "[msgId=" + msgId + ", topic=" + topic + ", tag=" + tag + ", bizKey=" + bizKey + "]";
	}
    
	

}
