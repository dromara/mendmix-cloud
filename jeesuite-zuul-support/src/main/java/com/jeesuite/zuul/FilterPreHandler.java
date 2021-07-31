package com.jeesuite.zuul;

import javax.servlet.http.HttpServletRequest;

public interface FilterPreHandler {

	void process(HttpServletRequest request);
	
}
