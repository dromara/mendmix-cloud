/*
 * Copyright 2016-2020 www.jeesuite.com.
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
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.mendmix.common.CurrentRuntimeContext;
import com.mendmix.common.guid.GUID;
import com.mendmix.common.http.HttpRequestEntity;
import com.mendmix.common.http.HttpResponseEntity;
import com.mendmix.common.model.AuthUser;
import com.mendmix.common.util.BeanUtils;
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
@JsonInclude(Include.NON_NULL)
public class MQMessage {

	private String requestId;
	private String tenantId;
	private String businessUnitId;
	private String produceAppId;
	private String produceBy;
	private String statusCheckUrl;
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

	public MQMessage() {
	}

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
		this.topic = topic;
		this.tag = tag;
		this.bizKey = bizKey;
		if (body instanceof byte[]) {
			this.body = new String((byte[]) body, StandardCharsets.UTF_8);
		} else {
			this.body = body;
		}
	}

	public String getTenantId() {
		if (tenantId == null) {
			tenantId = CurrentRuntimeContext.getTenantId(false);
		}
		return tenantId;
	}

	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}
	
	public String getBusinessUnitId() {
		if(businessUnitId == null) {
			businessUnitId = CurrentRuntimeContext.getBusinessUnitId();
		}
		return businessUnitId;
	}

	public void setBusinessUnitId(String workUnitId) {
		this.businessUnitId = workUnitId;
	}

	public String getRequestId() {
		if (requestId == null) {
			requestId = CurrentRuntimeContext.getRequestId();
		}
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getProduceAppId() {
		return produceAppId;
	}

	public void setProduceAppId(String produceAppId) {
		this.produceAppId = produceAppId;
	}

	public String getProduceBy() {
		if (produceBy == null) {
			AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
			produceBy = currentUser == null ? null : currentUser.getId();
		}
		return produceBy;
	}

	public void setProduceBy(String produceBy) {
		this.produceBy = produceBy;
	}
	

	public String getStatusCheckUrl() {
		return statusCheckUrl;
	}

	public void setStatusCheckUrl(String statusCheckUrl) {
		this.statusCheckUrl = statusCheckUrl;
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
		return msgId;
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
	
	public String initMsgId() {
		if(msgId != null)return msgId;
		msgId = GUID.uuid();
		return msgId;
	}

	public void onProducerFinished(String msgId,int partition,long offset) {
		this.msgId = msgId;
		this.partition = partition;
		this.offset = offset;
	}
	
	public MQMessage setUserContextOnProduce() {
		if(requestId == null) {
			requestId = CurrentRuntimeContext.getRequestId(); 
		}
		if (tenantId == null) {
			tenantId = CurrentRuntimeContext.getTenantId(false);
		}
		if (produceBy == null) {
			AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
			produceBy = currentUser == null ? null : currentUser.getId();
		}
		return this;
	}
	
	public MQMessage setUserContextOnConsume() {
		if(StringUtils.isNotBlank(requestId)) {	
			CurrentRuntimeContext.setRequestId(requestId);
		}
		if(StringUtils.isNotBlank(tenantId)) {	
			CurrentRuntimeContext.setTenantId(tenantId);
		}
		if(StringUtils.isNotBlank(produceBy)) {	
			CurrentRuntimeContext.setAuthUser(new AuthUser(produceBy, null));
		}
		return this;
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
	
	
	public boolean originStatusCompleted() {
		if(StringUtils.isBlank(getStatusCheckUrl()))return true;
		HttpResponseEntity resp = HttpRequestEntity.get(getStatusCheckUrl()).queryParam("txId", txId).execute();
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
		return "MQMessage [requestId=" + requestId + ", tenantId=" + tenantId + ", topic=" + topic + ", bizKey="
				+ bizKey + ", partition=" + partition + ", offset=" + offset + ", consumeTimes=" + consumeTimes + "]";
	}

	public String logString() {
		return toString();
	}

}
