package com.jeesuite.zuul.filter.pre;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.springweb.client.RequestHeaderBuilder;
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
		String requrstId = request.getHeader(CustomRequestHeaders.HEADER_REQUEST_ID);
		if(StringUtils.isNotBlank(requrstId)){
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_REQUEST_ID,requrstId);
		}else{
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_REQUEST_ID, TokenGenerator.generate());
		}
		ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_GATEWAY_TOKEN,TokenGenerator.generateWithSign());
		
		RequestHeaderBuilder.getHeaders().forEach( (k,v) -> {
			ctx.addZuulRequestHeader(k,v);
		} );
		
		return null;
	}

	@Override
	public int order() {
		return 1;
	}

}
