package com.jeesuite.gateway.filter.pre;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.DigestUtils;
import com.jeesuite.common.util.JsonUtils;
import com.jeesuite.common.util.ParameterUtils;
import com.jeesuite.gateway.FilterConstants;
import com.jeesuite.gateway.filter.FilterHandler;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.gateway.zuul.auth.ZuulAuthAdditionHandler;
import com.jeesuite.gateway.zuul.filter.AbstractZuulFilter;


/**
 * 
 * 
 * <br>
 * Class Name   : SignatureRequestHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2021-04-23
 */
public class SignatureRequestHandler implements FilterHandler{

	
	private Map<String, String> appIdSecretMappings = new HashMap<String, String>();
	

	public SignatureRequestHandler(Map<String, String> configs) {
		this.appIdSecretMappings = configs;
		ZuulAuthAdditionHandler.setOpenApiEnabled(true);
	}

	@Override
	public ServerWebExchange process(ServerWebExchange exchange, BizSystemModule module) {
		
		ApiInfo apiInfo = module.getApiInfo(exchange.getRequest().getPath().value());
        if(apiInfo == null || !apiInfo.isOpenApi()) {
        	throw new JeesuiteBaseException("该接口未开放访问权限");
        }
        
        HttpHeaders headers = exchange.getRequest().getHeaders();
		String sign = headers.getFirst(FilterConstants.X_SIGN_HEADER);
		if(StringUtils.isBlank(sign))return null;
		if(StringUtils.isBlank(sign))return null;
		String timestamp = headers.getFirst(FilterConstants.TIMESTAMP_HEADER);
		String appId = headers.getFirst(FilterConstants.APP_ID_HEADER);
		
		
		if(StringUtils.isAnyBlank(timestamp,appId)) {
			throw new JeesuiteBaseException("认证头信息不完整");
		}
		
		String secret = appIdSecretMappings.get(appId);
		
		if(StringUtils.isBlank(secret)) {
			throw new JeesuiteBaseException("appId不存在");
		}
		
		Object body = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
		
		Map<String, Object> map = JsonUtils.toHashMap(body.toString(), Object.class);
		String signBaseString = StringUtils.trimToEmpty(ParameterUtils.mapToQueryParams(map))  + timestamp + secret;
		String expectSign = DigestUtils.md5(signBaseString);
		
		if(!expectSign.equals(sign)) {
			throw new JeesuiteBaseException("签名错误");
		}
		
		exchange.getAttributes().put(AbstractZuulFilter.CTX_IGNORE_AUTH, Boolean.TRUE);
		
		return exchange;
	}

	@Override
	public int order() {
		return 0;
	}

}
