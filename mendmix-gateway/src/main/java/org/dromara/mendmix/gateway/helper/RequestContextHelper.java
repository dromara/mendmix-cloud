/*
 * Copyright 2016-2022 dromara.org.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dromara.mendmix.gateway.helper;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CACHED_REQUEST_BODY_ATTR;

import java.net.URI;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import org.dromara.mendmix.cache.CacheUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.DigestUtils;
import org.dromara.mendmix.common.util.IpUtils;
import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.util.ParameterUtils;
import org.dromara.mendmix.common.util.TokenGenerator;
import org.dromara.mendmix.common.util.WebUtils;
import org.dromara.mendmix.gateway.CurrentSystemHolder;
import org.dromara.mendmix.gateway.GatewayConfigs;
import org.dromara.mendmix.gateway.GatewayConstants;
import org.dromara.mendmix.gateway.GatewayConstants.UserClientType;
import org.dromara.mendmix.gateway.model.BizSystemModule;

import reactor.core.publisher.Mono;

/**
 * 
 * <br>
 * Class Name : RuequestHelper
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 14, 2022
 */
public class RequestContextHelper {


	private static Logger logger = LoggerFactory.getLogger("org.dromara.mendmix.adapter.gateway");
	
	private static final String WEBSOCKET_KEYS = "sec-websocket-key";
	private static final String CACHED_REQUEST_BODY_STR = "cachedRequestBodyString";
	private static final String OAUTH_TOKEN_CHECK_KEY_PREFIX = "oauth-token-check:";
	private static List<String> browerUserAgentKeys = Arrays.asList("Mozilla","Windows","Chrome","Safari","Opera","Android","AppleWebKit");
	private static String[] mobileUserAgentKeys = {"Android","iPhone","iPad"};
	private static String wxUserAgentKey = "MicroMessenger";
	private static String miniProgramUserAgentKey = "miniProgram";
	
	public static UserClientType getUserClientType(ServerHttpRequest request) {
		String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);
		if (userAgent == null) {
			return UserClientType.pc;
		}
		
		if(userAgent.contains(wxUserAgentKey)) {
			if(userAgent.contains(miniProgramUserAgentKey)) {
				return UserClientType.miniProgram;
			}else {
				return UserClientType.wxh5;
			}
		}
		
		for (String key : mobileUserAgentKeys) {
			if(userAgent.contains(key)) {
				return UserClientType.app;
			}
		}
		
		return UserClientType.pc;
	}
	
	public static String getOriginRequestUri(ServerWebExchange exchange) {
		String uri = exchange.getAttribute(GatewayConstants.CONTEXT_CURRENT_ORTIGN_URI);
		if(uri != null)return uri;
		return exchange.getRequest().getPath().value();
	}

	public static String getOriginDomain(ServerHttpRequest request) {
		String originUrl = request.getHeaders().getFirst(HttpHeaders.REFERER);
		if (originUrl == null) {
			originUrl = request.getHeaders().getOrigin();
		}
		String originDomain = null;
		if (originUrl != null) {
			originDomain = WebUtils.getDomain(originUrl);
		}
		return StringUtils.defaultString(originDomain, request.getURI().getAuthority());
	}

	public static String getIpAddr(ServerHttpRequest request) {
		String ip = ThreadLocalContext.get(GatewayConstants.CONTEXT_CURRENT_IP);
		if(StringUtils.isNotBlank(ip))return ip;
		ip = request.getHeaders().getFirst(IpUtils.HEADER_FROWARDED_FOR);
		if (StringUtils.isBlank(ip) || IpUtils.UNKNOWN.equalsIgnoreCase(ip)) {
			ip = request.getHeaders().getFirst("Proxy-Client-IP");
		}
		if (StringUtils.isBlank(ip) || IpUtils.UNKNOWN.equalsIgnoreCase(ip)) {
			ip = request.getHeaders().getFirst("WL-Proxy-Client-IP");
		}
		/**
		 * 对于通过多个代理的情况， 第一个IP为客户端真实IP,多个IP按照','分割 x-forwarded-for=192.168.2.22,
		 * 192.168.1.92
		 */
		if (ip != null && ip.length() > 15) {
			String[] ips = StringUtils.split(ip, ",");
			for (String _ip : ips) {
				ip = StringUtils.trimToNull(_ip);
				if (!IpUtils.UNKNOWN.equalsIgnoreCase(ip)) {
					break;
				}
			}
		}
		if (ip == null) {
			ip = request.getRemoteAddress().getAddress().getHostAddress();
		}
		// 0:0:0:0:0:0:0:1
		if (ip.contains(GlobalConstants.COLON)) {
			ip = IpUtils.LOCAL_BACK_IP;
		}
		ThreadLocalContext.set(GatewayConstants.CONTEXT_CURRENT_IP, ip);
		return ip;
	}
	
	public static String getRequestId(ServerWebExchange exchange) {
		String requestId = exchange.getRequest().getHeaders().getFirst(CustomRequestHeaders.HEADER_REQUEST_ID);
		if(StringUtils.isBlank(requestId) && exchange.getAttributes().containsKey(CustomRequestHeaders.HEADER_REQUEST_ID)) {
			requestId = exchange.getAttribute(CustomRequestHeaders.HEADER_REQUEST_ID);
		}
		if(StringUtils.isBlank(requestId)) {
			requestId = CurrentRuntimeContext.getRequestId();
		}else {
			CurrentRuntimeContext.setRequestId(requestId);
		}
		if(requestId != null) {
			exchange.getAttributes().put(CustomRequestHeaders.HEADER_REQUEST_ID, requestId);
		}
		return requestId;
	}
	
	public static String getCurrentRouteName(ServerWebExchange exchange) {
		String currentRoute = exchange.getAttribute(GatewayConstants.CONTEXT_CURRENT_ROUTE);
		if(currentRoute != null)return currentRoute;
		currentRoute = resolveRouteName(exchange.getRequest().getPath().value());
		exchange.getAttributes().put(GatewayConstants.CONTEXT_CURRENT_ROUTE, currentRoute);
		return currentRoute;
	}
	
	public static String resolveRouteName(String uri) {
		String contextPath = GatewayConfigs.PATH_PREFIX;
		int indexOf = StringUtils.indexOf(uri, GlobalConstants.PATH_SEPARATOR, contextPath.length());
		uri = uri.substring(indexOf + 1);

		List<String> routeNames = CurrentSystemHolder.getRouteNames();
		for (String routeName : routeNames) {
			if (uri.startsWith(routeName + "/")) {
				return routeName;
			}
		}
		return GlobalContext.APPID;
	}
	
	public static BizSystemModule getCurrentModule(ServerWebExchange exchange) {
		BizSystemModule module = exchange.getAttribute(GatewayConstants.CONTEXT_CURRENT_MODULE);
		if(module != null)return module;
		String routeName = getCurrentRouteName(exchange);
		module = CurrentSystemHolder.getModule(routeName);
		if(module != null) {
			exchange.getAttributes().put(GatewayConstants.CONTEXT_CURRENT_MODULE, module);
		}
		return module;
	}
	
	public static ApiInfo getCurrentApi(ServerWebExchange exchange) {
		ApiInfo api = exchange.getAttribute(GatewayConstants.CONTEXT_CURRENT_API);
		if(api != null)return api;
		BizSystemModule module = getCurrentModule(exchange);
		String uri = RequestContextHelper.getOriginRequestUri(exchange);
		ApiInfo apiInfo = module.getApiInfo(exchange.getRequest().getMethodValue(), uri);
		if(apiInfo != null) {
			exchange.getAttributes().put(GatewayConstants.CONTEXT_CURRENT_API, apiInfo);
		}
		return apiInfo;
	}

	public static boolean isWebSocketRequest(ServerHttpRequest request) {
		return request.getHeaders().containsKey(WEBSOCKET_KEYS);
	}
	
	public static boolean isJsonRequest(ServerHttpRequest request) {
		if(request.getMethod() == HttpMethod.GET)return false;
		MediaType contentType = request.getHeaders().getContentType();
		return contentType != null && contentType.includes(MediaType.APPLICATION_JSON);
	}
	
	public static boolean isServerSendEventRequest(ServerHttpRequest request) {
		List<MediaType> accept = request.getHeaders().getAccept();
		if(accept == null || accept.isEmpty()) {
			return false;
		}
		return MediaType.TEXT_EVENT_STREAM.equals(accept.get(0));
	}
	
	public static final boolean isMultipartContent(ServerHttpRequest request) {
		if (!HttpMethod.POST.name().equalsIgnoreCase(request.getMethodValue())) {
			return false;
		}
		MediaType contentType = request.getHeaders().getContentType();
		if (contentType == null) {
			return false;
		}
		
		if(MediaType.MULTIPART_FORM_DATA.getType().equals(contentType.getType())) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isBackendCall(ServerWebExchange exchange) {
		if(exchange.getAttributes().containsKey(GatewayConstants.CONTEXT_BACKEND_REQUEST)) {
			return (boolean) exchange.getAttributes().get(GatewayConstants.CONTEXT_BACKEND_REQUEST);
		}
		HttpHeaders headers = exchange.getRequest().getHeaders();
		boolean res = GatewayConfigs.openApiEnabled && (
				headers.containsKey(CustomRequestHeaders.HEADER_OPEN_SIGN));
		if(!res) {
			String userAgent = headers.getFirst(HttpHeaders.USER_AGENT);
			res = StringUtils.isBlank(userAgent) || !browerUserAgentKeys.stream().anyMatch(o -> userAgent.contains(o));
			if(isDebugMode(exchange)) {
				logger.info("<trace_logging> current userAgent:{},isBackendCall:{}",userAgent,res);
			}
		}
		exchange.getAttributes().put(GatewayConstants.CONTEXT_BACKEND_REQUEST, res);
		return res;
	}
	

	public static boolean withValidatedOAuthToken(ServerHttpRequest request) {
		String token = request.getQueryParams().getFirst(GlobalConstants.PARAM_AUTH_CODE);
		if(StringUtils.isBlank(token)) {
			token = request.getHeaders().getFirst(CustomRequestHeaders.HEADER_INVOKE_TOKEN);
		}
		if(StringUtils.isBlank(token)) {
			return false;
		}
		try {
			long expiredAt = TokenGenerator.validate(token, true);
			String reCheckKey = OAUTH_TOKEN_CHECK_KEY_PREFIX + DigestUtils.md5(token);
			long timeout = (expiredAt - System.currentTimeMillis() + 1)/1000;
			String lastValue = CacheUtils.getStr(reCheckKey);
			final String currentValue = request.getPath().value();
			if(CurrentRuntimeContext.isDebugMode()) {
				logger.info("<trace_logging> reCheckKey:{},lastValue:{},currentValue:{}",reCheckKey,lastValue,currentValue);
			}
			if(lastValue == null) {
				CacheUtils.setStr(reCheckKey, currentValue, timeout);
			}else if(!lastValue.equals(currentValue)) {
				logger.info(">>token 重复使用,reCheckKey:{},lastValue:{},currentValue:{}",reCheckKey,lastValue,currentValue);
        		return false;
			}
			return true;
		} catch (Exception e) {}
		
		return false;
	}
	
	public static String getCachingBodyString(ServerWebExchange exchange) {
    	if(exchange.getRequest().getMethod() == HttpMethod.GET || isMultipartContent(exchange.getRequest())) {
    		return null;
    	}
    	String bodyString = exchange.getAttribute(CACHED_REQUEST_BODY_STR);
    	if(bodyString != null)return bodyString;
		DataBuffer dataBuffer = exchange.getAttribute(CACHED_REQUEST_BODY_ATTR);
		if(dataBuffer == null)return null;
		CharBuffer charBuffer = StandardCharsets.UTF_8.decode(dataBuffer.asByteBuffer());
        bodyString = charBuffer.toString();
        //
        exchange.getAttributes().put(CACHED_REQUEST_BODY_STR, bodyString);
        exchange.getAttributes().put(GatewayConstants.CONTEXT_REQUEST_BODY_SIZE, charBuffer.length());
        //
        if(exchange.getAttributes().containsKey(GlobalConstants.DEBUG_TRACE_PARAM_NAME)) {
			logger.info(">>>>>>>>request body>>>>>>>>\n:{}",bodyString);
		}
		return bodyString;
	}
	
	public static String getCurrentRequestHitKey(ServerWebExchange exchange,boolean isolatingUser) {
		String hitKey = exchange.getAttribute(GatewayConstants.CONTEXT_CURRENT_HIT_KEY);
		if(hitKey != null)return hitKey;
		
		ServerHttpRequest request = exchange.getRequest();
		
		StringBuilder builder = new StringBuilder();
		builder.append(request.getMethodValue()).append(getOriginRequestUri(exchange));
		
		Map<String, Object> paramMap = new HashMap<>();
		request.getQueryParams().forEach( (k,v) -> {
			if(!v.isEmpty())paramMap.put(k, v.get(0));
		} );
		if(request.getMethod() != HttpMethod.GET) {
			String cachingBody = getCachingBodyString(exchange);
			if(StringUtils.isNotBlank(cachingBody) && cachingBody.length() > 2) {
				paramMap.putAll(JsonUtils.toHashMap(cachingBody,Object.class));
			}
		}
		if(!paramMap.isEmpty()) {
			builder.append(ParameterUtils.mapToQueryParams(paramMap));
		}
		
		builder.append(CurrentRuntimeContext.getSystemId());
		builder.append(CurrentRuntimeContext.getTenantId());
		if(isolatingUser) {
			AuthUser authUser = CurrentRuntimeContext.getCurrentUser();
			if(authUser != null) {
				builder.append(authUser.getId());
			}
		}
		
		hitKey = builder.length() <= 64 ? builder.toString() : DigestUtils.md5(builder.toString());
		exchange.getAttributes().put(GatewayConstants.CONTEXT_CURRENT_HIT_KEY, hitKey);
		return hitKey;
	}
	
	public static Locale getLocale(ServerHttpRequest request) {
		List<Locale> locales = request.getHeaders().getAcceptLanguageAsLocales();
		if(locales != null && !locales.isEmpty()) {
			return locales.get(0);
		}
		return Locale.CHINA;
	}
	
	public static String getTimestamp(ServerWebExchange exchange) {
		String timestamp = exchange.getRequest().getHeaders().getFirst(CustomRequestHeaders.HEADER_TIMESTAMP);
		if(timestamp == null) {
			timestamp = String.valueOf(System.currentTimeMillis());
			exchange.getAttributes().put(CustomRequestHeaders.HEADER_TIMESTAMP, timestamp);
		}
		return timestamp;
	}
	
	public static boolean isDebugMode(ServerWebExchange exchange) {
		if(exchange == null) {
			return ThreadLocalContext.exists(GlobalConstants.DEBUG_TRACE_PARAM_NAME);
		}
		return exchange.getAttributes().containsKey(GlobalConstants.DEBUG_TRACE_PARAM_NAME);
	}
	
	public static Mono<Void> writeData(ServerWebExchange exchange,byte[] data,int statusCode){
		ServerHttpResponse response = exchange.getResponse();
		response.setRawStatusCode(statusCode);
		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
		return response.writeWith(Mono.just(response.bufferFactory().wrap(data)));
	}
	
	public static void clearContextAttributes(ServerWebExchange exchange) {
		if(ThreadLocalContext.exists(GatewayConstants.CONTEXT_CLEARED_CONTEXT_ATTR)) {
			ThreadLocalContext.remove(GatewayConstants.CONTEXT_CLEARED_CONTEXT_ATTR);
			return;
		}
		Object attribute = exchange.getAttributes().remove(CACHED_REQUEST_BODY_ATTR);
		if (attribute != null && attribute instanceof PooledDataBuffer) {
			PooledDataBuffer dataBuffer = (PooledDataBuffer) attribute;
			if (dataBuffer.isAllocated()) {
			  dataBuffer.release();
			}
		}
		exchange.getAttributes().clear();
		ThreadLocalContext.unset();
	}
	
    public static void clearCustomContextAttributes(ServerWebExchange exchange) {
    	Set<String> keys = exchange.getAttributes().keySet();
    	for (String key : keys) {
			if(key.startsWith(GatewayConstants.CONTEXT_NAME_PREFIX) || key.startsWith(CustomRequestHeaders.HEADER_PREFIX)) {
				exchange.getAttributes().remove(key);
			}
		}
	}
    
    public static void setContextAttr(ServerWebExchange exchange,String key,Object value) {
    	if(exchange != null) {
    		exchange.getAttributes().put(key, value);
    	}
    	ThreadLocalContext.set(key, value);
    }
    
    public static <T> T getContextAttr(ServerWebExchange exchange,String key,T...defValue) {
    	T val = ThreadLocalContext.get(key);
    	if(val == null && exchange != null) {
    		val = exchange.getAttribute(key);
    	}
    	if(val == null && defValue != null && defValue.length > 0) {
    		val = defValue[0];
    	}
    	return val;
    }
    
    public static boolean hasContextAttr(ServerWebExchange exchange,String key) {
    	boolean has = false;
    	if(exchange != null) {
    		has = exchange.getAttributes().containsKey(key);
    	}
    	if(!has) {
    		has = ThreadLocalContext.exists(key);
    	}
    	return has;
    }
    
    
    public static URI rewriteRouteUri(ServerWebExchange exchange,BizSystemModule module,String rewriteBaseUrl) {
    	URI orignUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
    	String path = orignUri.getPath();
    	String query = orignUri.getQuery();
    	
    	URI newUri;
    	String newPath;
    	//uri重写
    	if(StringUtils.isBlank(rewriteBaseUrl)) {
    		newUri = orignUri;
    		newPath = module.getContextPath();
    	}else {
    		newUri = URI.create(rewriteBaseUrl);
    		newPath = newUri.getPath();
    	}
    	//
        String uriPrefix = module.getUriPrefix();
        if(module.getResolveRouteNames().size() > 1) {
        	uriPrefix = new StringBuilder(GatewayConfigs.PATH_PREFIX) //
        			.append(GlobalConstants.PATH_SEPARATOR) //
        			.append(module.getCurrentRouteName(exchange)) //
        			.toString();
        }
        if(module.getRewriteUriPrefix() != null) {
        	if(StringUtils.isBlank(GatewayConfigs.PATH_PREFIX)) {
        		path = module.getRewriteUriPrefix() + path;
        	}else {      
        		//非平台服务子网关对应多个路由
        		if(module.getRewriteUriPrefix().equals(module.getContextPath())) {
        			if(module.isSubGateway()) {        				
        				path = StringUtils.replace(path, GatewayConfigs.PATH_PREFIX, module.getRewriteUriPrefix(), 1);
        			}else {
        				path = StringUtils.replace(path, module.getCurrentRouteName(exchange), module.getRewriteUriPrefix(), 1);
        			}
        		}else {        			
        			path = StringUtils.replace(path, uriPrefix, module.getRewriteUriPrefix(), 1);
        		}
        	}
        }else {
        	if(!path.startsWith(uriPrefix)) {
        		if(uriPrefix.startsWith(newPath)) {
        			path = uriPrefix + path;
        		}else {
        			if(StringUtils.isBlank(GatewayConfigs.PATH_PREFIX)) {
        				path = newPath + uriPrefix + path;
        			}else {        				
        				path = StringUtils.replace(uriPrefix, GatewayConfigs.PATH_PREFIX, newPath, 1) + path;
        			}
        		}
            }
        }
		//
    	newUri = UriComponentsBuilder
    			       .fromUri(orignUri)  //
    			       .scheme(newUri.getScheme()) //
    			       .host(newUri.getHost()) //
    			       .port(newUri.getPort())
    			       .replacePath(path)  //
    			       .replaceQuery(query) //
    			       .build(false)  //
    			       .toUri();
		exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, newUri);
		if(RequestContextHelper.isDebugMode(exchange)) {
    		logger.info(">>rewriteRouteUri>> \n -orignUri:{}\n -mappingUri:{}\n -newUri:{}",orignUri,rewriteBaseUrl,newUri);
    	}
        return newUri;
    }
    
    public static String getRequestTenantId(ServerWebExchange exchange) {
		String tenantId = CurrentRuntimeContext.getTenantId();
		if(StringUtils.isBlank(tenantId)) {
			final ServerHttpRequest request = exchange.getRequest();
			tenantId = request.getHeaders().getFirst(CustomRequestHeaders.HEADER_TENANT_ID);
			if(StringUtils.isBlank(tenantId) && CurrentRuntimeContext.getCurrentUser() == null) {
				tenantId = request.getQueryParams().getFirst(GlobalConstants.PARAM_TENANT_ID);
				if(StringUtils.isBlank(tenantId) && isJsonRequest(request)) {
					String bodyString = getCachingBodyString(exchange);
					if(JsonUtils.isJsonObjectString(bodyString)) {
						tenantId = JsonUtils.getJsonNodeValue(bodyString, GlobalConstants.PARAM_TENANT_ID);
					}
				}
			}
			if(tenantId != null) {
				CurrentRuntimeContext.setTenantId(tenantId);
			}
		}
		return tenantId;
	}
    
    public static String buildServiceChainHeader(HttpHeaders headers) {
		String newHeaderVal = headers.getFirst(CustomRequestHeaders.HEADER_SERVICE_CHAIN);
		String curNode = new StringBuilder(GlobalContext.APPID)
				.append(GlobalConstants.AT)
				.append(GlobalContext.getNodeName())
				.toString();
		if (StringUtils.isBlank(newHeaderVal)) {
			newHeaderVal = curNode;
		}else {
			newHeaderVal = new StringBuilder(newHeaderVal).append(GlobalConstants.COMMA).append(curNode).toString();
		}
		return newHeaderVal;
	}
}
