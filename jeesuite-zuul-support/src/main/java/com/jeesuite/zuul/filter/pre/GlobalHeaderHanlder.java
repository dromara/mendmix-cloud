package com.jeesuite.zuul.filter.pre;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.GlobalRuntimeContext;
import com.jeesuite.common.util.IpUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.zuul.filter.FilterHandler;
import com.jeesuite.zuul.model.BizSystemModule;
import com.netflix.zuul.context.RequestContext;


/**
 * 
 * 
 * <br>
 * Class Name   : GlobalHeaderHanlder
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020年9月15日
 */
public class GlobalHeaderHanlder implements FilterHandler {

	@Override
	public Object process(RequestContext ctx,HttpServletRequest request, BizSystemModule module) {
		
		String invokeIp = request.getHeader(CustomRequestHeaders.HEADER_INVOKER_IP);
		if(StringUtils.isNotBlank(invokeIp)){
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_INVOKER_IP,invokeIp);
		}else{
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_INVOKER_IP,IpUtils.getLocalIpAddr());
		}
		//
		String invokeApp = request.getHeader(CustomRequestHeaders.HEADER_INVOKER_APP_ID);
		if(StringUtils.isNotBlank(invokeApp)){
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_INVOKER_APP_ID,invokeApp);
		}else{				
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_INVOKER_APP_ID,GlobalRuntimeContext.APPID);
		}
		//
		String authToken = request.getHeader(CustomRequestHeaders.HEADER_AUTH_TOKEN);
		if(StringUtils.isNotBlank(authToken)){
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_AUTH_TOKEN,authToken);
		}else{
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_AUTH_TOKEN, TokenGenerator.generateWithSign());
		}
		//
		String requrstId = request.getHeader(CustomRequestHeaders.HEADER_REQUEST_ID);
		if(StringUtils.isNotBlank(requrstId)){
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_REQUEST_ID,requrstId);
		}else{
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_REQUEST_ID, TokenGenerator.generate());
		}
		//
		String authUser = request.getHeader(CustomRequestHeaders.HEADER_AUTH_USER);
		if(StringUtils.isNotBlank(authUser) && StringUtils.isNotBlank(authToken)){
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_AUTH_USER,authUser);
		}
		
		String tenantId = CurrentRuntimeContext.getTenantId(false);
		if(tenantId != null){				
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_TENANT_ID, tenantId);
		}
		//
		ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_GATEWAY_TOKEN,TokenGenerator.generateWithSign());
		
		return null;
	}

	@Override
	public int order() {
		return 1;
	}

}
