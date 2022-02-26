package com.jeesuite.gateway.zuul.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.util.WebUtils;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;


/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年6月12日
 */
public class ZuulGlobalErrorFilter extends ZuulFilter {

	/**
	 * 
	 */
	private static final String THROWABLE_KEY = "throwable";

	private static final String ERROR_JSON_TEMPLATE = "{\"code\": 9999,\"msg\":\"%s\"}";

	private static Logger log = LoggerFactory.getLogger("com.zvos.xgroup.apigateway.filter");
	
	@Override
	public String filterType() {
		return "post";
	}

	@Override
	public int filterOrder() {
		return 0;
	}

	@Override
    public boolean shouldFilter() {
		return RequestContext.getCurrentContext().containsKey(THROWABLE_KEY);
	}

	@Override
	public Object run() {		
		RequestContext ctx = RequestContext.getCurrentContext();
		
		String errorMemo = null;
		String message = null;
		String exceptionName = null;
		try {
			Throwable ex = (Exception) ctx.get(THROWABLE_KEY);
			if(ex instanceof com.netflix.zuul.exception.ZuulException){
				while(ex.getCause() != null && ex.getCause().getClass() != ex.getClass()){
					ex = ex.getCause();
				}
			}
			
			message = ex.getMessage();
			if(ex instanceof java.net.ConnectException){
				errorMemo = "连接失败,请重试";
			}else if(ex instanceof java.net.SocketTimeoutException){
				errorMemo = "请求超时,请重试";
			}
			
			exceptionName = ex.getClass().getName();
			log.error("process_request ERROR -> exception:{},message:{}",exceptionName,message);
	        WebUtils.responseOutJson(ctx.getResponse(), String.format(ERROR_JSON_TEMPLATE, errorMemo));
		} catch (Exception e) {
			log.error("process_request FORWARD_ERROR",e);
			WebUtils.responseOutJson(ctx.getResponse(), String.format(ERROR_JSON_TEMPLATE, errorMemo));
		}
		ctx.setSendZuulResponse(false);
        return null;

	}
}
