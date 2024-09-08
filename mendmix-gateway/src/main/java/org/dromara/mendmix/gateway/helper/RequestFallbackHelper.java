package org.dromara.mendmix.gateway.helper;

import java.util.HashMap;
import java.util.Map;

import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.http.HttpMethod;
import org.dromara.mendmix.common.http.HttpRequestEntity;
import org.dromara.mendmix.common.http.HttpResponseEntity;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.gateway.GatewayConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月16日
 */
public class RequestFallbackHelper {

	private final static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.adapter.gateway");

	public static Map<String, Object> handleForwardUrl(ServerWebExchange exchange,String forwardUrl) {
		ServerHttpRequest request = exchange.getRequest();
		Map<String, Object> result;
		//兼容直接返回固定内容的情况
		if(!forwardUrl.startsWith(GatewayConstants.HTTP_SCHAME)) {
			result = new HashMap<>(2);
			result.put(GlobalConstants.PARAM_CODE, 200);
			result.put(GlobalConstants.PARAM_DATA, JsonUtils.toHashMap(forwardUrl,Object.class));
			return result;
		}
		
		boolean traceMode = logger.isTraceEnabled() || exchange.getAttributes().containsKey(GlobalConstants.DEBUG_TRACE_PARAM_NAME);
		
		String requestUri = RequestContextHelper.getOriginRequestUri(exchange);
		logger.debug("<startup-logging> request[{}] forward to[{}] Begin...",requestUri,forwardUrl);
		HttpRequestEntity requestEntity = HttpRequestEntity.create(HttpMethod.valueOf(request.getMethodValue())).uri(forwardUrl);
		MultiValueMap<String, String> queryParams = request.getQueryParams();
		if(!queryParams.isEmpty()) {
			for (String paramName : queryParams.keySet()) {
				requestEntity.queryParam(paramName, queryParams.getFirst(paramName));
			}
		}
		if(!request.getMethodValue().equals(HttpMethod.GET.name())) {
			String body = RequestContextHelper.getCachingBodyString(exchange);
			requestEntity.body(body);
			if(traceMode) {
				logger.info("handleForwardUrl request body:\n{}",body);
			}
		}
		//兼容feign调用的一些逻辑
		if(request.getHeaders().containsKey(HttpHeaders.USER_AGENT)) {
			requestEntity.header(HttpHeaders.USER_AGENT, request.getHeaders().getFirst(HttpHeaders.USER_AGENT));
		}
		HttpResponseEntity responseEntity = requestEntity.useContext().execute();
		if(traceMode) {
			logger.info("handleForwardUrl response:\n -statusCode:{}\n -body:{}",responseEntity.getStatusCode(),responseEntity.getBody());
		}
		if(!responseEntity.httpOk()) {
			throw new MendmixBaseException(responseEntity.getStatusCode(),responseEntity.getBizCode(), responseEntity.getMessage());
		}
		if(!JsonUtils.isJsonString(responseEntity.getBody())) {
			Map<String, Object> nullValue = new HashMap<>(1);
			nullValue.put(GatewayConstants.NULL_BODY_KEY, null);
			return nullValue;
		}
		
		if(JsonUtils.isJsonArrayString(responseEntity.getBody())) {
			result = new HashMap<>(2);
			result.put(GlobalConstants.PARAM_CODE, 200);
			result.put(GlobalConstants.PARAM_DATA, JsonUtils.toList(responseEntity.getBody(), Map.class));
		}else {
			Map<String, Object> respMap = JsonUtils.toHashMap(responseEntity.getBody(), Object.class);
			if(respMap.containsKey(GlobalConstants.PARAM_CODE) && (
					respMap.size() == 1 ||
					respMap.containsKey(GlobalConstants.PARAM_DATA) || 
					respMap.containsKey(GlobalConstants.PARAM_MSG)
					)
		    ) {
				result = respMap;
			}else {
				result = new HashMap<>(2);
				result.put(GlobalConstants.PARAM_CODE, 200);
				result.put(GlobalConstants.PARAM_DATA, respMap);
			}
		}
		if(traceMode) {
		   logger.info("<startup-logging> request[{}] forward successed",requestUri);
		}
		return result;
	}

}
