package com.jeesuite.gateway.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import com.jeesuite.gateway.zuul.filter.global.GlobalAdditionHandler;
import com.jeesuite.security.SecurityDelegatingFilter;  
  
@Configuration  
public class FilterRegConfig {  

	
	@Bean
	public FilterRegistrationBean<SecurityDelegatingFilter> securityDelegatingFilter() {
	    FilterRegistrationBean<SecurityDelegatingFilter> registration = new FilterRegistrationBean<>();
	    SecurityDelegatingFilter filter = new SecurityDelegatingFilter();
	    filter.setAdditionHandler(new GlobalAdditionHandler());
		registration.setFilter(filter);
	    registration.addUrlPatterns("/*");
	    registration.setName("authFilter");
	    registration.setOrder(0);
	    return registration;
	} 
	
	@Bean
	@ConditionalOnProperty(value = "jeesuite.request.cors.enabled", havingValue = "true",matchIfMissing = false)
	public FilterRegistrationBean<CorsFilter> corsFilter() {
	    FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>();
	    
	    CorsConfiguration configuration = new CorsConfiguration();
		configuration.addAllowedOrigin("*");
		configuration.addAllowedHeader("*");
		configuration.addAllowedMethod("*");
		configuration.setAllowCredentials(true);
		configuration.applyPermitDefaultValues();
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

	    registration.setFilter(new CorsFilter(source));
	    registration.addUrlPatterns("/*");
	    registration.setName("corsFilter");
	    registration.setOrder(0);
	    
	    return registration;
	}
 
	
}  
