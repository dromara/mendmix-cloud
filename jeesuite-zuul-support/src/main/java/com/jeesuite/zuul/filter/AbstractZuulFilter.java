package com.jeesuite.zuul.filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.springweb.model.WrapperResponse;
import com.jeesuite.zuul.FilterConstants;
import com.jeesuite.zuul.filter.post.ResponseLogHandler;
import com.jeesuite.zuul.filter.post.ResponseRewriteHandler;
import com.jeesuite.zuul.filter.pre.GlobalHeaderHandler;
import com.jeesuite.zuul.filter.pre.RequestLogHandler;
import com.jeesuite.zuul.filter.pre.SignatureRequestHandler;
import com.jeesuite.zuul.model.BizSystemModule;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public abstract class AbstractZuulFilter extends ZuulFilter {

	public static String CTX_IGNORE_AUTH = "_ctx_ignore_auth_";
	
	protected List<FilterHandler> handlers = new ArrayList<>();
	
	private String filterType;
	private int filterOrder;

	public AbstractZuulFilter(String filterType,FilterHandler...filterHandlers) {
		this.filterType = filterType;
		if("pre".equals(filterType)) {
			this.filterOrder = 0;
			//
			handlers.add(new GlobalHeaderHandler());
			if(ResourceUtils.getBoolean("jeesuite.actionLog.collector.enabled")) {
				handlers.add(new RequestLogHandler());
			}
			Map<String, String> configs = ResourceUtils.getMappingValues("jeesuite.openapi.secret.mapping");
			if(!configs.isEmpty()) {
				handlers.add(new SignatureRequestHandler(configs));
			}
		}else if("post".equals(filterType)) {
			this.filterOrder = 9;
			//
			if(ResourceUtils.getBoolean("jeesuite.actionLog.collector.enabled")) {
				handlers.add(new ResponseLogHandler());
			}
			if(ResourceUtils.getBoolean("jeesuite.response.rewrite.enabled", true)) {
				handlers.add(new ResponseRewriteHandler());
			}
		} 
		if(filterHandlers != null && filterHandlers.length > 0 && filterHandlers[0] != null) {
			for (FilterHandler filterHandler : filterHandlers) {
				handlers.add(filterHandler);
			}
		}
		//
		if(handlers.size() > 1) {			
			handlers.stream().sorted(Comparator.comparing(FilterHandler::order));
		}
	}

	@Override
	public boolean shouldFilter() {
		if(handlers.isEmpty())return false;
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();
		boolean skip = HttpMethod.OPTIONS.name().equals(request.getMethod()) 
				|| HttpMethod.HEAD.name().equals(request.getMethod()) 
				|| ctx.getBoolean(FilterConstants.CONTEXT_IGNORE_FILTER);
		
		return !skip ;
	}
	
	@Override
	public Object run() {
		
		RequestContext ctx = RequestContext.getCurrentContext();
		if (!ctx.sendZuulResponse())
			return null;
		
		HttpServletRequest request = ctx.getRequest();
		BizSystemModule module = (BizSystemModule) ctx.get(FilterConstants.CONTEXT_ROUTE_SERVICE);
		
		try {
			for (FilterHandler handler : handlers) {
				handler.process(ctx, request, module);
			}
		} catch (Exception e) {
			int code = (e instanceof JeesuiteBaseException) ? ((JeesuiteBaseException)e).getCode() : 500;
			ctx.setResponseBody(WrapperResponse.buildErrorJSON(code, e.getMessage()));
			ctx.setResponseStatusCode(503);
			return null;
		}
		
		return null;
	}

	@Override
	public String filterType() {
		return filterType;
	}

	@Override
	public int filterOrder() {
		return filterOrder;
	}

}
