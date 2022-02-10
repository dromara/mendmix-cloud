package com.jeesuite.zuul.filter.pre;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import com.google.common.io.CharStreams;
import com.jeesuite.common.http.HttpMethod;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.logging.integrate.ActionLog;
import com.jeesuite.logging.integrate.ActionLogCollector;
import com.jeesuite.zuul.filter.FilterHandler;
import com.jeesuite.zuul.model.BizSystemModule;
import com.netflix.zuul.context.RequestContext;

public class RequestLogHandler implements FilterHandler {

	@Override
	public Object process(RequestContext ctx, HttpServletRequest request, BizSystemModule module) {
		
		ActionLog actionLog = ActionLogCollector.currentActionLog();
		if(actionLog == null)return null;
		actionLog.setModuleId(module.getServiceId());
		
		ApiInfo apiInfo = module.getApiInfo(request.getRequestURI());
        if(apiInfo != null && !apiInfo.isRequestLog()) {
        	return null;
        }
        actionLog.setQueryParameters(request.getQueryString());
        if(HttpMethod.POST.name().equals(request.getMethod())) {
        	try {
        		String data = CharStreams.toString(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8));
        		actionLog.setRequestData(data);
        	} catch (Exception e) {}
        	
        }

		return null;
	}

	@Override
	public int order() {
		return 0;
	}

}
