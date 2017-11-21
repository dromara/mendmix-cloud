package com.jeesuite.spring;

import org.springframework.web.context.WebApplicationContext;

/**
 * context初始化完成监听器接口
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年11月20日
 */
public interface ContextIinitializedListener {

	void onContextIinitialized(WebApplicationContext applicationContext);
}
