package com.jeesuite.zuul.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.ThreadLocalContext;
import com.jeesuite.common.WebConstants;
import com.jeesuite.common.util.WebUtils;
import com.jeesuite.zuul.CurrentSystemHolder;
import com.jeesuite.zuul.FilterConstants;
import com.jeesuite.zuul.filter.pre.AuthCheckHandler;
import com.jeesuite.zuul.model.BizSystemModule;
import com.jeesuite.springweb.CurrentRuntimeContext;
import com.jeesuite.springweb.model.WrapperResponse;
import com.netflix.zuul.context.RequestContext;

/**
 * 
 * <br>
 * Class Name   : GlobalContextFilter
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年6月17日
 */
public class GlobalContextFilter implements Filter {
	
	private static AuthCheckHandler authCheckHandler = new AuthCheckHandler();

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		//非接口
		if(request.getRequestURI().contains(WebConstants.DOT)) {
			chain.doFilter(req, res);
			return ;
		}
		
		ThreadLocalContext.unset();
		
		CurrentRuntimeContext.init(request, response);
		
		String contextPath = CurrentRuntimeContext.getContextPath();
		int indexOf = StringUtils.indexOf(request.getRequestURI(), WebConstants.PATH_SEPARATOR, contextPath.length());
		String routeName = StringUtils.split(request.getRequestURI().substring(indexOf + 1), WebConstants.PATH_SEPARATOR)[0];
		//
		BizSystemModule module = CurrentSystemHolder.getModule(routeName);
		if(module != null) {
			RequestContext context = RequestContext.getCurrentContext();
			try {				
				authCheckHandler.process(context,request, module);
			} catch (JeesuiteBaseException e) {
				WebUtils.responseOutJson(response, WrapperResponse.buildErrorJSON(e.getCode(), e.getMessage()));
				return;
			}
			//
			context.put(FilterConstants.CONTEXT_ROUTE_SERVICE, module);
		}
		//
		chain.doFilter(req, res);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		Filter.super.init(filterConfig);
	}
	
	

}
