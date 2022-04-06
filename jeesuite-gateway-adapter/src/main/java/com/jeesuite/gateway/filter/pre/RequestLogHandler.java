package com.jeesuite.gateway.filter.pre;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;

import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.gateway.filter.FilterHandler;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.logging.integrate.ActionLog;
import com.jeesuite.logging.integrate.ActionLogCollector;

public class RequestLogHandler implements FilterHandler {

	private boolean ignoreBody = ResourceUtils.getBoolean("jeesuite.actionLog.requestBody.ignore",true);
	
	@Override
	public ServerWebExchange process(ServerWebExchange exchange, BizSystemModule module) {
		
		ActionLog actionLog = ActionLogCollector.currentActionLog();
		if(actionLog == null)return null;
		actionLog.setModuleId(module.getServiceId());
		
		ServerHttpRequest request = exchange.getRequest();
		ApiInfo apiInfo = module.getApiInfo(request.getPath().value());
        if(apiInfo != null && !apiInfo.isRequestLog()) {
        	return null;
        }
        actionLog.setQueryParameters(JsonUtils.toJson(request.getQueryParams()));
        if(!ignoreBody && HttpMethod.POST.equals(request.getMethod()) && !isMultipartRequest(request)) {
        	try {
        		String data = exchange.getAttribute(ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR);
        		actionLog.setRequestData(data);
        	} catch (Exception e) {}
        	
        }

		return null;
	}

	@Override
	public int order() {
		return 0;
	}
	
	private boolean isMultipartRequest(ServerHttpRequest request) {
		MediaType mediaType = request.getHeaders().getContentType();
		return mediaType.getType().equals(MediaType.MULTIPART_RELATED.getType())
				|| mediaType.equals(MediaType.APPLICATION_OCTET_STREAM);
	}

}
