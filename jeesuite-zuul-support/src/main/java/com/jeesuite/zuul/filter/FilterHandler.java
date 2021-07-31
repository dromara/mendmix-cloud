package com.jeesuite.zuul.filter;

import javax.servlet.http.HttpServletRequest;

import com.jeesuite.zuul.model.BizSystemModule;
import com.netflix.zuul.context.RequestContext;

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

	Object process(RequestContext ctx,HttpServletRequest request,BizSystemModule module);
	
	int order();
	
}
