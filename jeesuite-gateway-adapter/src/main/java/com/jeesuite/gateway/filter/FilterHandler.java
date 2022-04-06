package com.jeesuite.gateway.filter;

import org.springframework.web.server.ServerWebExchange;

import com.jeesuite.gateway.model.BizSystemModule;

/**
 * 
 * 
 * <br>
 * Class Name   : PreFilterHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020年9月15日
 */
public interface FilterHandler {

	ServerWebExchange process(ServerWebExchange exchange,BizSystemModule module);
	
	int order();
	
}
