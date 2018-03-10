package com.jeesuite.spring.web;

import java.util.Map;

import javax.servlet.ServletContextEvent;

import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.jeesuite.spring.ContextIinitializedListener;
import com.jeesuite.spring.InstanceFactory;
import com.jeesuite.spring.SpringInstanceProvider;



public class ContextLoaderListener extends org.springframework.web.context.ContextLoaderListener {

	@Override
	public void contextInitialized(ServletContextEvent event) {
		String serviceName = event.getServletContext().getInitParameter("appName");
		System.setProperty("serviceName", serviceName == null ? "undefined" : serviceName);
		super.contextInitialized(event);
		WebApplicationContext applicationContext = WebApplicationContextUtils.getRequiredWebApplicationContext(event.getServletContext());
		SpringInstanceProvider provider = new SpringInstanceProvider(applicationContext);
		InstanceFactory.loadFinished(provider);
		
		Map<String, ContextIinitializedListener> interfaces = applicationContext.getBeansOfType(ContextIinitializedListener.class);
		if(interfaces != null){
			for (ContextIinitializedListener listener : interfaces.values()) {
				System.out.println(">>>begin to execute listener:"+listener.getClass().getName());
				listener.onContextIinitialized(applicationContext);
				System.out.println("<<<<finish execute listener:"+listener.getClass().getName());
			}
		}
	}
}
