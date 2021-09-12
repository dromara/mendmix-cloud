package com.jeesuite.springweb.configure;

import java.nio.charset.Charset;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

import com.jeesuite.common.util.ResourceUtils;

@Configuration
@ConditionalOnProperty(name = "response.force-charset.enbaled",havingValue = "true",matchIfMissing = true)
public class ResponseEncodingConfig extends WebMvcConfigurationSupport {

    @Override
	protected void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
    	
    	Charset charset = Charset.forName(ResourceUtils.getProperty("response.force-charset.name","UTF-8"));
    	
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
}

