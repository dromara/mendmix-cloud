package com.jeesuite.gateway.filter.post;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.server.ServerWebExchange;

import com.jeesuite.gateway.filter.FilterHandler;
import com.jeesuite.gateway.model.BizSystemModule;

public class ResponseRewriteHandler implements FilterHandler {
	
	private static Logger log = LoggerFactory.getLogger("com.jeesuite.gateway.filter");
	
	private static final String DEFAULT_ERROR_MSG = "系统繁忙";
	private static final String _MESSAGE_NAME = "message";
	private static final String _MSG_NAME = "msg";
	private static final String _CODE_NAME = "code";
	private static final String _DATA_NAME = "data";
	

	@Override
	public ServerWebExchange process(ServerWebExchange exchange, BizSystemModule module) {
		
		return null;
	}

	@Override
	public int order() {
		return 1;
	}
	
}
