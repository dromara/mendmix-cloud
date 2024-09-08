/**
 * 
 */
package org.dromara.mendmix.gateway.model;

import java.io.Serializable;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.gateway.GatewayConstants.HitType;
import org.dromara.mendmix.gateway.helper.RequestContextHelper;
import org.springframework.web.server.ServerWebExchange;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * <br>
 * @author vakinge
 * @date 2023年9月14日
 */
public class RatelimitStrategy implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private String id;
	private String serviceId; 
	private String uriKey; //请求地址
	private double permitsPerSecond = 1;
	private String hitType;
	private List<String> hitValues;
	
	@JsonIgnore
	private Pattern uriPattern;
	
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
		if(uriKey != null && uriKey.contains("/*") && uriKey.length() > 2) {
			String regex = StringUtils.replace(uriKey, "/*", "/.*");
			uriPattern = Pattern.compile(regex);
		}
	}
	public double getPermitsPerSecond() {
		return permitsPerSecond;
	}
	public void setPermitsPerSecond(double permitsPerSecond) {
		this.permitsPerSecond = permitsPerSecond;
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
	public Pattern getUriPattern() {
		return uriPattern;
	}
	
	public String resolveHitKey(ServerWebExchange exchange,String serviceId,ApiInfo apiInfo) {
		if(hitType == null || HitType.any.name().equals(hitType)) {
			return HitType.any.name();
		}
		String currentKey = null;
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
		if(currentKey == null || (hitValues != null && !hitValues.isEmpty() && !hitValues.contains(currentKey))) {
			return null;
		}
		return new StringBuilder(serviceId).append(apiInfo.getIdentifier()).append(hitType).append(currentKey).toString();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hitType == null) ? 0 : hitType.hashCode());
		result = prime * result + ((serviceId == null) ? 0 : serviceId.hashCode());
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
		RatelimitStrategy other = (RatelimitStrategy) obj;
		if (hitType == null) {
			if (other.hitType != null)
				return false;
		} else if (!hitType.equals(other.hitType))
			return false;
		if (serviceId == null) {
			if (other.serviceId != null)
				return false;
		} else if (!serviceId.equals(other.serviceId))
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
