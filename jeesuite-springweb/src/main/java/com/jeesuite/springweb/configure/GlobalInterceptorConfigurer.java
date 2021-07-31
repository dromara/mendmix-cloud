package com.jeesuite.springweb.configure;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.springweb.CurrentRuntimeContext;
import com.jeesuite.springweb.interceptor.GlobalDefaultInterceptor;

@Configuration
public class GlobalInterceptorConfigurer implements WebMvcConfigurer {

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new GlobalDefaultInterceptor())
		             .addPathPatterns("/**")
		             .excludePathPatterns("/error","/swagger-ui.html","/v2/api-docs","/swagger-resources/**","/webjars/**","/info","/health");
        
        if("local".equals(ResourceUtils.getProperty("jeesuite.configcenter.profile"))){
        	registry.addInterceptor(new MockLoginUserInterceptor()).addPathPatterns("/**");
        }
	}
	
	private class MockLoginUserInterceptor extends HandlerInterceptorAdapter {

        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                throws Exception {
        	
        	if(CurrentRuntimeContext.getCurrentUser() == null){
        		AuthUser authUser = new AuthUser();
        		authUser.setId("1");
        		authUser.setUsername("admin");
        		CurrentRuntimeContext.setAuthUser(authUser);
        	}
        	return true;
        }
    }
}
