package com.jeesuite.monitor.web.hanlder;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jfinal.handler.Handler;

public class GlobalsHandler extends Handler {
	
	private static final String _TOKEN = "_token_";
	
	@Override
	public void handle(String target, HttpServletRequest request,
			HttpServletResponse response, boolean[] isHandled) {
		
		String requestURI = request.getRequestURI();

		//判断是否ajax请求
    	String header = request.getHeader("X-Requested-With");
        boolean isAjax = "XMLHttpRequest".equalsIgnoreCase(header);
        if (!isAjax) {
        	request.setAttribute("path", request.getContextPath());
        	// 非ajax请求设置防重复提交令牌
			request.setAttribute(_TOKEN, UUID.randomUUID().toString().replaceAll("\\-", ""));
		}
 
        next.handle(target, request, response, isHandled);
	}
}