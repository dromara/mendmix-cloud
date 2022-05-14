package com.jeesuite.gateway.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import com.jeesuite.gateway.GatewayConstants;
import com.jeesuite.gateway.security.GatewayReactiveCustomAuthnHandler;
import com.jeesuite.security.ReactiveSecurityDelegatingFilter;  
  
@Configuration  
public class FilterRegConfig {
	
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ConditionalOnProperty(name = "application.cors.enabled",havingValue = "true")
	public CorsWebFilter corsWebFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		CorsConfiguration corsConfiguration = new CorsConfiguration();
		corsConfiguration.addAllowedHeader("*");
		corsConfiguration.addAllowedMethod("*");
		corsConfiguration.addAllowedOriginPattern("*");
		corsConfiguration.setAllowCredentials(true);

		source.registerCorsConfiguration("/**", corsConfiguration);

		return new CorsWebFilter(source);
	}
	
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE + 1)
	public ReactiveSecurityDelegatingFilter securityDelegatingFilter() {
		return new ReactiveSecurityDelegatingFilter(new GatewayReactiveCustomAuthnHandler(), GatewayConstants.PATH_PREFIX);
	}
}  
