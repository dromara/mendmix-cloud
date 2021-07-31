package com.jeesuite.zuul.filter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpMethod;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.zuul.FilterConstants;
import com.jeesuite.zuul.filter.post.ResponseRewriteHandler;
import com.jeesuite.zuul.filter.pre.GlobalHeaderHanlder;
import com.jeesuite.zuul.model.BizSystemModule;
import com.jeesuite.springweb.model.WrapperResponse;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;

public abstract class AbstractZuulFilter extends ZuulFilter {

	protected List<FilterHandler> hanlders = new ArrayList<>();
	
	private String filterType;
	private int filterOrder;

	public AbstractZuulFilter(String filterType,FilterHandler...filterHandlers) {
		this.filterType = filterType;
		if("pre".equals(filterType)) {
			this.filterOrder = 0;
			//
			hanlders.add(new GlobalHeaderHanlder());
		}else if("post".equals(filterType)) {
			this.filterOrder = 9;
			//
			hanlders.add(new ResponseRewriteHandler());
		} 
		//
		if(hanlders.size() > 1) {			
			hanlders.stream().sorted(Comparator.comparing(FilterHandler::order));
		}
	}

	@Override
	public boolean shouldFilter() {
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
			for (FilterHandler handler : hanlders) {
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
