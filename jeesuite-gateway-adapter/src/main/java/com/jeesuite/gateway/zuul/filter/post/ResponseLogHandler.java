package com.jeesuite.gateway.zuul.filter.post;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.gateway.zuul.filter.FilterHandler;
import com.jeesuite.logging.integrate.ActionLog;
import com.jeesuite.logging.integrate.ActionLogCollector;
import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;

public class ResponseLogHandler implements FilterHandler {

	@Override
	public Object process(RequestContext ctx, HttpServletRequest request, BizSystemModule module) {
		
		int statusCode = ctx.getResponseStatusCode();
		if(statusCode != 200)return null;
		
		ActionLog actionLog = ActionLogCollector.currentActionLog();
		if(actionLog == null)return null;
		
		List<Pair<String, String>> headers = ctx.getOriginResponseHeaders();
		for (Pair<String, String> pair : headers) {
			if (CustomRequestHeaders.HEADER_EXCEPTION_CODE.equals(pair.first())) {
				actionLog.setResponseCode(Integer.parseInt(pair.second()));
				break;
			}
		}
		
		ApiInfo apiInfo = module.getApiInfo(request.getRequestURI());
        if(apiInfo != null && !apiInfo.isResponseLog()) {
        	return null;
        }
        
        ResponseCompose responseCompose = new ResponseCompose(ctx);
        ctx.set(ResponseCompose.class.getName(), responseCompose);
        actionLog.setResponseData(responseCompose.getBodyString());
		
		return null;
	}

	@Override
	public int order() {
		return 0;
	}

}
