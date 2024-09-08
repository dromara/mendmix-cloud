/**
 * 
 */
package org.dromara.mendmix.gateway.model;

import java.io.Serializable;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.gateway.GatewayConstants.FallbackStrategy;
import org.dromara.mendmix.gateway.GatewayConstants.HitType;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.springframework.web.server.ServerWebExchange;


/**
 * <br>
 * @author vakinge
 * @date 2023年8月22日
 */
public class FallbackRule implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private String id;
	private String serviceId;
	private String uriKey; //请求地址
	private boolean breakerMode;   //熔断模式
	private String strategy;
	private String fallbackSourceKey; //cache key,url
	private String fallbackContent;
	private String hitType;
	private List<String> hitValues;
	
	public FallbackRule() {}
	
	public FallbackRule(FallbackStrategy strategy) {
		this.strategy = strategy.name();
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getServiceId() {
		return serviceId;
	}

	public void setServiceId(String serviceId) {
		this.serviceId = serviceId;
	}

	public String getUriKey() {
		return uriKey;
	}
	public void setUriKey(String uriKey) {
		this.uriKey = uriKey;
	}
	public boolean isBreakerMode() {
		return breakerMode;
	}
	public void setBreakerMode(boolean breakerMode) {
		this.breakerMode = breakerMode;
	}
	public String getStrategy() {
		return strategy;
	}
	public void setStrategy(String strategy) {
		this.strategy = strategy;
	}
	public String getFallbackSourceKey() {
		return fallbackSourceKey;
	}
	public void setFallbackSourceKey(String fallbackSourceKey) {
		this.fallbackSourceKey = fallbackSourceKey;
	}
	public String getFallbackContent() {
		return fallbackContent;
	}
	public void setFallbackContent(String fallbackContent) {
		this.fallbackContent = fallbackContent;
	}
	public String getHitType() {
		return hitType;
	}
	public void setHitType(String hitType) {
		this.hitType = hitType;
	}
	public List<String> getHitValues() {
		return hitValues;
	}
	public void setHitValues(List<String> hitValues) {
		this.hitValues = hitValues;
	}

	public boolean currentMatch(ServerWebExchange exchange) {
		String currentKey = null;
		if(hitType == null || HitType.any.name().equals(hitType)) {
			return true;
		}
		if(HitType.clientIp.name().equals(hitType)) {
			currentKey = RequestContextHelper.getIpAddr(exchange.getRequest());
		}else if(HitType.user.name().equals(hitType)) {
			currentKey =  CurrentRuntimeContext.getCurrentUserId();
		}else if(HitType.tenant.name().equals(hitType)) {
			currentKey = CurrentRuntimeContext.getTenantId();
		}else if(HitType.system.name().equals(hitType)) {
			currentKey = CurrentRuntimeContext.getSystemId();
			if(currentKey == null) {
				currentKey = exchange.getRequest().getHeaders().getFirst(CustomRequestHeaders.HEADER_OPEN_APP_ID);
			}
		}
		if(currentKey == null) {
			return false;
		}
		return hitValues == null || hitValues.isEmpty() || hitValues.contains(currentKey);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hitType == null) ? 0 : hitType.hashCode());
		result = prime * result + ((uriKey == null) ? 0 : uriKey.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FallbackRule other = (FallbackRule) obj;
		if (hitType == null) {
			if (other.hitType != null)
				return false;
		} else if (!hitType.equals(other.hitType))
			return false;
		if (uriKey == null) {
			if (other.uriKey != null)
				return false;
		} else if (!uriKey.equals(other.uriKey))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,ToStringStyle.JSON_STYLE);
	}
	
	
}
