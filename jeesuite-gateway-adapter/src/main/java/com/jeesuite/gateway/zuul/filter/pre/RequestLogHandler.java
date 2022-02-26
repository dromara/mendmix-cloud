package com.jeesuite.gateway.zuul.filter.pre;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import com.google.common.io.CharStreams;
import com.jeesuite.common.http.HttpMethod;
import com.jeesuite.common.model.ApiInfo;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.gateway.zuul.filter.FilterHandler;
import com.jeesuite.logging.integrate.ActionLog;
import com.jeesuite.logging.integrate.ActionLogCollector;
import com.netflix.zuul.context.RequestContext;

public class RequestLogHandler implements FilterHandler {

	private boolean ignoreBody = ResourceUtils.getBoolean("jeesuite.actionLog.requestBody.ignore",true);
	
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
        if(!ignoreBody && HttpMethod.POST.name().equals(request.getMethod()) && !WebUtils.isMultipartContent(request)) {
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
