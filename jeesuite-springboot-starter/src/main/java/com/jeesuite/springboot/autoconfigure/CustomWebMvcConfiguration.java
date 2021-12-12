package com.jeesuite.springboot.autoconfigure;

import java.nio.charset.Charset;
import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.springweb.interceptor.GlobalDefaultInterceptor;
import com.jeesuite.springweb.interceptor.MockLoginUserInterceptor;

@Configuration
public class CustomWebMvcConfiguration implements WebMvcConfigurer {

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {

		Charset charset = Charset.forName(ResourceUtils.getProperty("response.force-charset.name", "UTF-8"));

		for (HttpMessageConverter<?> converter : converters) {
			// 解决controller返回普通文本中文乱码问题
			if (converter instanceof StringHttpMessageConverter) {
				((StringHttpMessageConverter) converter).setDefaultCharset(charset);
			}
			// 解决controller返回json对象中文乱码问题
			if (converter instanceof MappingJackson2HttpMessageConverter) {
				((MappingJackson2HttpMessageConverter) converter).setDefaultCharset(charset);
			}
		}
		System.out.println(">>setDefaultCharset:" + charset);
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new GlobalDefaultInterceptor())
		       .addPathPatterns("/**")
		       .excludePathPatterns("/error","/swagger-ui.html", "/v2/api-docs", "/swagger-resources/**", "/webjars/**", "/info", "/health");

		if ("local".equals(ResourceUtils.getProperty("jeesuite.configcenter.profile"))) {
			registry.addInterceptor(new MockLoginUserInterceptor()).addPathPatterns("/**");
		}
	}

}
