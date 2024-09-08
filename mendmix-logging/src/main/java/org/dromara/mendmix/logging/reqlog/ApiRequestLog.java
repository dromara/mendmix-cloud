/**
 * 
 */
package org.dromara.mendmix.logging.reqlog;

import java.io.Serializable;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.common.guid.GUID;
import org.dromara.mendmix.common.model.ApiInfo;
import org.dromara.mendmix.common.model.AuthUser;
import org.dromara.mendmix.common.util.BeanUtils;
import org.dromara.mendmix.common.util.IpUtils;
import org.dromara.mendmix.logging.LogConstants;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * <br>
 * @author vakinge
 * @date 2024年5月20日
 */
public class ApiRequestLog implements Serializable{

	private static final long serialVersionUID = 1L;
	public static final String API_REQUEST_ACTION_GROUP = "apiRequest";
	private static BasicInfo basicInfo;
	
	static {
		basicInfo = BeanUtils.mapToBean(LogConstants.SERVICE_INFO, BasicInfo.class);
	}
	
	private String id;
	private String traceId;
	private BasicInfo basic;
	private ContextInfo context;
	private RequestInfo request;
	private ResponseInfo response;
	private Date startTime;
	private Date endTime;
	private Integer useTime = 0;
	private boolean successed;
	private String exceptions;
	private String actionGroup;
	private String actionName;
	private String actionKey;
	
	private long timestamp;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTraceId() {
		return traceId;
	}

	public void setTraceId(String traceId) {
		this.traceId = traceId;
	}

	public ContextInfo getContext() {
		return context;
	}

	public void setContext(ContextInfo context) {
		this.context = context;
	}

	public BasicInfo getBasic() {
		return basic;
	}

	public void setBasic(BasicInfo basic) {
		this.basic = basic;
	}

	public RequestInfo getRequest() {
		return request;
	}

	public void setRequest(RequestInfo request) {
		this.request = request;
	}

	public ResponseInfo getResponse() {
		return response;
	}

	public void setResponse(ResponseInfo response) {
		this.response = response;
	}
	
	public String getActionGroup() {
		return actionGroup;
	}

	public void setActionGroup(String actionGroup) {
		this.actionGroup = actionGroup;
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}
    
	public String getActionKey() {
		return actionKey;
	}

	public void setActionKey(String actionKey) {
		this.actionKey = actionKey;
	}

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Integer getUseTime() {
		return useTime;
	}

	public void setUseTime(Integer useTime) {
		this.useTime = useTime;
	}
	
	public String getExceptions() {
		return exceptions;
	}

	public void setExceptions(String exceptions) {
		this.exceptions = exceptions;
	}

	public boolean isSuccessed() {
		return successed;
	}

	public void setSuccessed(boolean successed) {
		this.successed = successed;
	}
	
	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public static ApiRequestLog build(HttpServletRequest request,ApiInfo apiInfo) {
		String clienIp = IpUtils.getIpAddr(request);
		String method = request.getMethod();
		String uri = request.getRequestURI();
		Map<String, String> headers = new LinkedHashMap<>(LogConstants.LOGGING_REQUEST_HEADERS.size());
		Enumeration<String> headerNames = request.getHeaderNames();
		String headerName;
		while(headerNames.hasMoreElements()) {
			headerName = headerNames.nextElement();
			if(!LogConstants.LOGGING_REQUEST_HEADERS.contains(headerName))continue;
			headers.put(headerName, request.getHeader(headerName));
		}
		String parameters = request.getQueryString();
		Object body = null;
		if(apiInfo == null 
				|| apiInfo.isRequestLog() 
				|| request.getHeader(CustomRequestHeaders.HEADER_REQUEST_BODY_LOGGING) != null) {
			body = ThreadLocalContext.get(GlobalConstants.CONTEXT_REQUEST_BODY_KEY);
		}
		return build(apiInfo, clienIp, method, uri, headers, parameters, body);
	}
	
	public static ApiRequestLog build(ServerHttpRequest request,ApiInfo apiInfo ) {
		String clienIp = IpUtils.getIpAddr(request);
		String method = request.getMethodValue();
		String uri = request.getPath().value();
		final HttpHeaders requestHeaders = request.getHeaders();
		Map<String, String> headers = new LinkedHashMap<>(LogConstants.LOGGING_GATEWAY_REQUEST_HEADERS.size());
		for (String headerName : LogConstants.LOGGING_GATEWAY_REQUEST_HEADERS) {
			if(requestHeaders.containsKey(headerName)) {
				headers.put(headerName, requestHeaders.getFirst(headerName));
			}
		}
		String parameters = request.getURI().getRawQuery();
		return build(apiInfo, clienIp, method, uri, headers, parameters, null);
	}
	
	public static ApiRequestLog build(String actionGroup,String actionKey,String actionName) {
		ApiRequestLog log = new ApiRequestLog();
		log.traceId = GUID.uuid();
		log.startTime = new Date();
		log.timestamp = log.startTime.getTime();
		log.basic = basicInfo;
		//
		log.context = new ContextInfo();
		log.context.systemId = CurrentRuntimeContext.getSystemId();
		log.context.tenantId = CurrentRuntimeContext.getTenantId();
		log.setActionGroup(actionGroup);
		log.setActionName(actionName);
		log.setActionKey(actionKey);
		return log;
	}
	
	private static ApiRequestLog build(ApiInfo apiInfo,String clienIp,String method,String uri,Map<String, String> headers,String parameters,Object body) {
		ApiRequestLog log = new ApiRequestLog();
		log.traceId = CurrentRuntimeContext.getRequestId();
		log.startTime = new Date();
		log.timestamp = log.startTime.getTime();
		log.basic = basicInfo;
		//
		log.context = new ContextInfo();
		log.context.systemId = CurrentRuntimeContext.getSystemId();
		log.context.tenantId = CurrentRuntimeContext.getTenantId();
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null) {
			log.context.userId = currentUser.getId();
			log.context.userName = currentUser.getName();
			log.context.principalId = currentUser.getPrincipalId();
		}
		log.context.clientType = CurrentRuntimeContext.getClientType();
		//
		log.request = new RequestInfo();
		log.request.clientIp = clienIp;
		log.request.method = method;
		log.request.uri = uri;
		log.actionGroup = API_REQUEST_ACTION_GROUP;
		if(apiInfo != null) {
			log.request.apiIdentifier = StringUtils.defaultString(apiInfo.getRoutedIdentifier(),apiInfo.getIdentifier());
			log.actionKey = log.request.apiIdentifier;
			//
			if(!GlobalContext.isGateway() && apiInfo.isActionLog()) {				
				log.actionName = apiInfo.getName();
			}
		}
		log.request.headers = headers;
		log.request.parameters = parameters;
		log.request.body = body;
		return log;
	}
	
	public void end(Throwable throwable,int httpStatus,Map<String, String> headers,Object body) {
		this.successed = throwable != null ? false : httpStatus < 400;
		this.endTime = new Date();
		this.useTime = (int) (this.endTime.getTime()  - this.startTime.getTime());
		if(this.response == null)this.response = new ResponseInfo();
		this.response.headers = headers;
		if(headers != null && headers.containsKey(CustomRequestHeaders.HEADER_ORIGIN_HTTP_STATUS)) {
			this.response.httpStatus = Integer.parseInt(headers.get(CustomRequestHeaders.HEADER_ORIGIN_HTTP_STATUS));
		}else {			
			this.response.httpStatus = httpStatus;
		}
		if(body != null)this.response.body = body;
	}
	
	public void setResponseBody(Object body) {
		if(this.response == null) {
			this.response = new ResponseInfo();
		}
		this.response.body = body;
	}
	
	public void updateContextInfo(String systemId,String tenantId,AuthUser authUser) {
		if(context == null)context = new ContextInfo();
		context.systemId = systemId;
		context.tenantId = tenantId;
		if(authUser != null) {
			context.userId = authUser.getId();
			context.userName = authUser.getName();
			context.principalId = authUser.getPrincipalId();
		}
	}
	
	public void setRequestBody(Object body) {
		if(this.request == null) {
			this.request = new RequestInfo();
		}
		this.request.body = body;
	}

	public static class ContextInfo {
		public String systemId;
		public String tenantId;
		public String userId;
		public String userName;
		public String principalId;
		public String clientType;
	}
	
	public static class BasicInfo {
		public boolean gateway;
		public String timezone;
		public int timeOffset;
		public String env;
		public String systemKey;
		public String systemId;
		public String serviceId;
		public String podName;
		public String podIp;
	}
	
	public static class RequestInfo {
		public String clientIp;
		public String method;
		public String uri;
		public String apiIdentifier;
		public Map<String, String> headers;
		public String parameters;
		public Object body;
	}
	
	public static class ResponseInfo {
		public int httpStatus;
		public Map<String, String> headers;
		public Object body;
	}
}
