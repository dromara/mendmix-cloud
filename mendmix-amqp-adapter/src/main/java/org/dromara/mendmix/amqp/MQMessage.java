/*
 * Copyright 2016-2020 dromara.org.
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
package org.dromara.mendmix.amqp;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.guid.GUID;
import org.dromara.mendmix.common.http.HttpRequestEntity;
import org.dromara.mendmix.common.http.HttpResponseEntity;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.JsonUtils;

/**
 * 
 * <br>
 * Class Name : MQMessage
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年7月11日
 */
@JsonInclude(Include.NON_NULL)
public class MQMessage {

	private static final String SKYWALKING_HEADER_PREFIX = "sw";
	
	private String txId;
	private String topic;
	private String tag;
	private String bizKey;
	private Object body;
	@JsonIgnore
	private Long processTime; //处理时间
	private Long deliverTime; //定时消息
	//=======================
	private String msgId;
	private Integer partition;
	@JsonIgnore
	private long offset;
	private int consumeTimes;
	@JsonIgnore
	private Object originMessage;
	@JsonIgnore
	private String partitionKey;
	private Map<String, String> headers;

	public MQMessage() {
	}

	public static MQMessage build(String json) {
		MQMessage message = JsonUtils.toObject(json, MQMessage.class);
		if (!BeanUtils.isSimpleDataType(message.body.getClass())) {
			message.setBody(JsonUtils.toJson(message.body));
		}
		//新老切换兼容 ，保留一个版本
		if(message.getTenantId() == null) {
			String tenantId = JsonUtils.getJsonNodeValue(json, GlobalConstants.PARAM_TENANT_ID);
		    if(tenantId != null) {
		    	message.addHeader(CustomRequestHeaders.HEADER_TENANT_ID, tenantId);
		    }
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
		this.topic = topic;
		this.tag = tag;
		this.bizKey = bizKey;
		if (body instanceof byte[]) {
			this.body = new String((byte[]) body, StandardCharsets.UTF_8);
		} else {
			this.body = body;
		}
	}

	@JsonIgnore
	public String getTenantId() {
		return getHeaderValue(CustomRequestHeaders.HEADER_TENANT_ID);
	}

	@JsonIgnore
	public String getBusinessUnitId() {
		return getHeaderValue(CustomRequestHeaders.HEADER_BUSINESS_UNIT_ID);
	}

	@JsonIgnore
	public String getRequestId() {
		return getHeaderValue(CustomRequestHeaders.HEADER_REQUEST_ID);
	}

	@JsonIgnore
	public String getProduceBy() {
		return getHeaderValue(CustomRequestHeaders.HEADER_USER_ID);
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
		this.topic = topic;
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

	public Long getDeliverTime() {
		return deliverTime;
	}

	public void setDeliverTime(Long deliverTime) {
		this.deliverTime = deliverTime;
	}
	
	public String getMsgId() {
		return StringUtils.defaultString(msgId, getHeaderValue(CustomRequestHeaders.HEADER_DATA_ID));
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}
	
	public String getTxId() {
		return txId;
	}

	public void setTxId(String txId) {
		this.txId = txId;
	}

	public Integer getPartition() {
		return partition;
	}

	public void setPartition(Integer partition) {
		this.partition = partition;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public int getConsumeTimes() {
		return consumeTimes;
	}

	public void setConsumeTimes(int consumeTimes) {
		this.consumeTimes = consumeTimes;
	}
	
	public String getPartitionKey() {
		return partitionKey;
	}

	public void setPartitionKey(String partitionKey) {
		this.partitionKey = partitionKey;
	}
	
	public Map<String, String> getHeaders() {
		return headers;
	}
	
	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public MQMessage addHeader(String name, String value) {
		if(StringUtils.isAnyBlank(name,value) || name.startsWith(SKYWALKING_HEADER_PREFIX)) {
			return this;
		}
		if(this.headers == null)this.headers = new HashMap<>();
		this.headers.put(name, value);
		return this;
	}
	
	public String getHeaderValue(String name) {
		return this.headers == null ? null : this.headers.get(name);
	}
	
	public boolean containsHeader(String name) {
		return this.headers == null ? false : this.headers.containsKey(name);
	}

	public void onProducerFinished(String msgId,int partition,long offset) {
		this.msgId = msgId;
		this.partition = partition;
		this.offset = offset;
	}
	
	public MQMessage initContextHeaders() {
		addHeader(CustomRequestHeaders.HEADER_DATA_ID, String.valueOf(GUID.guid()));
		String headerValue = CurrentRuntimeContext.getRequestId();
		if(headerValue != null) {
			addHeader(CustomRequestHeaders.HEADER_REQUEST_ID, headerValue);
		}
		headerValue = CurrentRuntimeContext.getTenantId(false);
		if(headerValue != null) {
			addHeader(CustomRequestHeaders.HEADER_TENANT_ID, headerValue);
		}
		headerValue = CurrentRuntimeContext.getBusinessUnitId();
		if(headerValue != null) {
			addHeader(CustomRequestHeaders.HEADER_BUSINESS_UNIT_ID, headerValue);
		}
		headerValue = CurrentRuntimeContext.getCurrentUserId();
		if(headerValue != null) {
			addHeader(CustomRequestHeaders.HEADER_USER_ID, headerValue);
		}
		return this;
	}
	
	public MQMessage setUserContextOnProduce() {
		initContextHeaders();
		return this;
	}
	
	public MQMessage setUserContextOnConsume() {
		String headerValue = getRequestId();
		if(StringUtils.isNotBlank(headerValue)) {	
			CurrentRuntimeContext.setRequestId(headerValue);
		}
		headerValue = getTenantId();
		if(StringUtils.isNotBlank(headerValue)) {	
			CurrentRuntimeContext.setTenantId(headerValue);
		}
		headerValue = getBusinessUnitId();
		if(StringUtils.isNotBlank(headerValue)) {	
			CurrentRuntimeContext.setBusinessUnitId(headerValue);
		}
		headerValue = getProduceBy();
		if(StringUtils.isNotBlank(headerValue)) {	
			CurrentRuntimeContext.setAuthUser(new AuthUser(headerValue, null));
		}
		return this;
	}
	
	public void setTenantId(String tenantId) {
		addHeader(CustomRequestHeaders.HEADER_TENANT_ID, tenantId);
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
		return JsonUtils.toJson(this);
	}

	public <T> T toObject(Class<T> clazz) {
		return JsonUtils.toObject(toMessageValue(true), clazz);
	}

	public <T> List<T> toList(Class<T> clazz) {
		return JsonUtils.toList(toMessageValue(true), clazz);
	}
	
	public void setOriginMessage(Object originMessage) {
		this.originMessage = originMessage;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T getOriginMessage(Class<?> clazz) {
		return (T) this.originMessage;
	}
	
	public boolean originStatusCompleted(String statusCheckUrl) {
		if(StringUtils.isBlank(statusCheckUrl))return true;
		HttpResponseEntity resp = HttpRequestEntity.get(statusCheckUrl).queryParam("txId", txId).execute();
		boolean completed = false;
		try {
			completed = Boolean.parseBoolean(resp.getUnwrapBody());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return completed;
		
	}

	

	@Override
	public String toString() {
		return "MQMessage [msgId=" + getMsgId() + ", topic=" + topic + ", bizKey=" + bizKey + ", headers=" + headers + "]";
	}

	public String logString() {
		return toString();
	}

}
