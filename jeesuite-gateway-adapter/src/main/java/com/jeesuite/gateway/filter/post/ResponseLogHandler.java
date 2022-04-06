package com.jeesuite.gateway.filter.post;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.gateway.filter.FilterHandler;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.logging.integrate.ActionLog;
import com.jeesuite.logging.integrate.ActionLogCollector;

public class ResponseLogHandler implements FilterHandler {

	private boolean ignoreBody = ResourceUtils.getBoolean("jeesuite.actionLog.responseBody.ignore",true);
	
	@Override
	public ServerWebExchange process(ServerWebExchange exchange, BizSystemModule module) {
		
		HttpStatus httpStatus = exchange.getResponse().getStatusCode();
		if(!httpStatus.is2xxSuccessful())return null;
		
		ActionLog actionLog = ActionLogCollector.currentActionLog();
		if(actionLog == null)return null;
		
		HttpHeaders httpHeaders = exchange.getResponse().getHeaders();
		if(httpHeaders.containsKey(CustomRequestHeaders.HEADER_EXCEPTION_CODE)) {
			actionLog.setResponseCode(Integer.parseInt(httpHeaders.getFirst(CustomRequestHeaders.HEADER_EXCEPTION_CODE)));
		}
		
		if(ignoreBody)return null;
		
		ApiInfo apiInfo = module.getApiInfo(exchange.getRequest().getPath().value());
        if(apiInfo != null && !apiInfo.isResponseLog()) {
        	return null;
        }
        
        
        //actionLog.setResponseData(responseCompose.getBodyString());
		
		return exchange;
	}

	@Override
	public int order() {
		return 0;
	}

}
