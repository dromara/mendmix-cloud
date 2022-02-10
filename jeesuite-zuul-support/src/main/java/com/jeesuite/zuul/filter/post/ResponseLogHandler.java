package com.jeesuite.zuul.filter.post;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.logging.integrate.ActionLog;
import com.jeesuite.logging.integrate.ActionLogCollector;
import com.jeesuite.zuul.filter.FilterHandler;
import com.jeesuite.zuul.model.BizSystemModule;
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
		
		return null;
	}

	@Override
	public int order() {
		return 0;
	}

}
