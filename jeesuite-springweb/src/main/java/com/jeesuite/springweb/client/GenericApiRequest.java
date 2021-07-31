package com.jeesuite.springweb.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.WebConstants;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.BeanUtils;
import com.jeesuite.common.util.IpUtils;
import com.jeesuite.common.util.ParameterUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.springweb.CurrentRuntimeContext;

import okhttp3.ConnectionPool;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 通用api请求工具 <br>
 * Class Name : GenericApiRequest
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年8月21日
 */
public class GenericApiRequest {

	private static Logger logger = LoggerFactory.getLogger("com.jeesuite.springweb.client");

	private static final String STANDARD_RSP_JSON_PREFIX = "{\"code\"";
	private static final String OPENGW_ERROR_RSP_JSON_PREFIX = "{\"errorCode\"";
	//private static String podName = System.getenv("POD_NAME");

	private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");
	private static final MediaType FROM_URLENCODED_MEDIA_TYPE = MediaType
			.parse("application/x-www-form-urlencoded; charset=utf-8");
	private static OkHttpClient httpClient;

	static {
		long timeout = ResourceUtils.getLong("generic.request.timeout.ms", 60000);
		httpClient = new OkHttpClient.Builder().connectionPool(new ConnectionPool(1, 30L, TimeUnit.SECONDS))
				.connectTimeout(5000, TimeUnit.MILLISECONDS).readTimeout(timeout, TimeUnit.MILLISECONDS)
				.writeTimeout(timeout, TimeUnit.MILLISECONDS).build();
	}

	private String requestUrl;
	private HttpMethod requestMethod;
	private MediaType mediaType;
	private Map<String, Object> requestParameters;
	private Map<String, String> headers;
	private Object requestData;
	private Class<?> responseClass;
	private TypeReference<?> responseTypeReference;

	private GenericApiRequest(String requestUrl, HttpMethod requestMethod,
			MediaType mediaType,Map<String, String> headers, Map<String, Object> requestParameters, Object requestData, Class<?> responseClass,
			TypeReference<?> responseTypeReference) {
		this.requestUrl = requestUrl;
		this.mediaType = mediaType;
		this.requestMethod = requestMethod;
		this.headers = headers;
		this.requestParameters = requestParameters;
		this.requestData = requestData;
		this.responseClass = responseClass;
		this.responseTypeReference = responseTypeReference;
	}

	@SuppressWarnings("unchecked")
	public <T> T waitResponse() {
		String responseBody = execute();
		if (responseBody == null)
			return null;
		if (!responseBody.startsWith(ParameterUtils.JSON_PREFIX)) {
			return (T) responseBody;
		}
		if (responseTypeReference != null) {
			return (T) JSON.parseObject(responseBody, responseTypeReference);
		}
		return (T) JSON.parseObject(responseBody, responseClass);
	}

	@SuppressWarnings("unchecked")
	public <T> List<T> waitResponseList() {
		String responseBody = execute();
		if (responseBody == null)
			return null;
		return (List<T>) JSON.parseArray(responseBody, responseClass);
	}

	private String execute() {

		String url = CustomRequestHostHolder.resolveUrl(requestUrl);
		HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
		if (requestParameters != null) {
			for (String key : requestParameters.keySet()) {
				urlBuilder.addQueryParameter(key, requestParameters.get(key).toString());
			}
		}

		okhttp3.Headers.Builder headerBuilder = new Headers.Builder();
		headerBuilder.add(WebConstants.HEADER_INVOKER_IP, IpUtils.getLocalIpAddr());
		headerBuilder.add(WebConstants.HEADER_AUTH_TOKEN, TokenGenerator.generateWithSign());

		String requestId = null;
		String invokerAppId = null;
		try {
			HttpServletRequest request = CurrentRuntimeContext.getRequest();
			invokerAppId = request.getHeader(WebConstants.HEADER_INVOKER_APP_ID);
			requestId = request.getHeader(WebConstants.HEADER_REQUEST_ID);
		} catch (Exception e) {
		}

		if (requestId == null)
			requestId = TokenGenerator.generate();
		if (invokerAppId == null)
			invokerAppId = CurrentRuntimeContext.APPID;

		headerBuilder.add(WebConstants.HEADER_REQUEST_ID, requestId);
		if (invokerAppId != null)
			headerBuilder.add(WebConstants.HEADER_INVOKER_APP_ID, invokerAppId);
		// 登录用户
		AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
		if(currentUser != null) {
			headerBuilder.add(WebConstants.HEADER_AUTH_USER, currentUser.toEncodeString());
		}
        //
		String tenantId = CurrentRuntimeContext.getTenantId(false);
		if (tenantId != null) {
			headerBuilder.add(WebConstants.HEADER_TENANT_ID, tenantId);
		}

		if(headers != null) {
			headers.forEach( (name,value) -> {
				headerBuilder.add(name, value);
			});
		}

		okhttp3.Request.Builder requestBuilder = new Request.Builder().headers(headerBuilder.build())
				.url(urlBuilder.build());

		String postJson = null;
		if (requestMethod == HttpMethod.POST && requestData != null) {
			RequestBody body;
			if (JSON_MEDIA_TYPE.equals(mediaType)) {
				postJson = (requestData instanceof String) ? requestData.toString() : JsonUtils.toJson(requestData);
				body = FormBody.create(JSON_MEDIA_TYPE, postJson);
			} else {
				Map<String, Object> requestDataMap;
				if (requestData instanceof Map) {
					requestDataMap = (Map<String, Object>) requestData;
				} else {
					requestDataMap = BeanUtils.beanToMap(requestData);
				}
				FormBody.Builder builder = new FormBody.Builder();
				requestDataMap.forEach((k, v) -> {
					if (v != null)
						builder.add(k, v.toString());
				});
				body = builder.build();
			}
			requestBuilder.post(body);
		}

		if (logger.isDebugEnabled()) {
			StringBuilder logData = new StringBuilder();
			logData.append("------call_remote_api_begin------\n");
			if (tenantId != null)
				logData.append("tenantId:").append(tenantId).append("\n");
			logData.append("url:").append(url).append("\n");
			logData.append("method:").append(requestMethod.name()).append("\n");
			if (postJson != null)
				logData.append("requestData:").append(postJson).append("\n");
			logger.debug(logData.toString());
		}

		String responseString = null;
		try {
			Response response = httpClient.newCall(requestBuilder.build()).execute();
			if (response.body() != null) {
				responseString = response.body().string();
				if (responseString.startsWith(STANDARD_RSP_JSON_PREFIX)) {
					JSONObject jsonObject = JSON.parseObject(responseString);
					int code = jsonObject.getIntValue(WebConstants.PARAM_CODE);
					if (code == 200) {
						responseString = jsonObject.getString(WebConstants.PARAM_DATA);
					} else {
						if (logger.isDebugEnabled())
							logger.debug("call_remote_api_error ->url:{},code:{},message:{}", url, code,
									jsonObject.getString("msg"));
						throw new JeesuiteBaseException(code, jsonObject.getString("msg"));
					}
				}else if(responseString.startsWith(OPENGW_ERROR_RSP_JSON_PREFIX)) {
					JSONObject jsonObject = JSON.parseObject(responseString);
					throw new JeesuiteBaseException(jsonObject.getString("msg"));
				}
			}
			if (response.isSuccessful()) {
				return responseString;
			} else if (response.isRedirect()) {
				throw new JeesuiteBaseException(302, response.message());
			} else {
				String message = response.message();
				if (StringUtils.isBlank(message)) {
					message = HttpStatus.valueOf(response.code()).getReasonPhrase();
				}
				if (logger.isDebugEnabled())
					logger.debug("call_remote_api_error ->url:{},code:{},message:{}", url, response.code(), message);
				throw new JeesuiteBaseException(response.code(), message);
			}
		} catch (Exception e) {
			StringBuilder errorMessage = new StringBuilder();
			errorMessage.append("------call_remote_api_error------\n");
			if (tenantId != null)
				errorMessage.append("tenantId:").append(tenantId).append("\n");
			errorMessage.append("url:").append(url).append("\n");
			errorMessage.append("method:").append(requestMethod.name()).append("\n");
			if (responseString != null)
				errorMessage.append("responseData:").append(responseString).append("\n");
			errorMessage.append("error:").append(e.getMessage());
			logger.error(errorMessage.toString());
			if (e instanceof JeesuiteBaseException)
				throw (JeesuiteBaseException) e;
			if (e instanceof java.net.SocketTimeoutException) {
				throw new JeesuiteBaseException(504, e.getMessage());
			}
			throw new JeesuiteBaseException("系统繁忙，稍后再试[004]");
		}

	}

	public static String get(String url, Map<String, Object> parameters) {
		HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
		if (parameters != null) {
			for (String key : parameters.keySet()) {
				urlBuilder.addQueryParameter(key, parameters.get(key).toString());
			}
		}

		okhttp3.Request.Builder requestBuilder = new Request.Builder().url(urlBuilder.build());
		try {
			Response response = httpClient.newCall(requestBuilder.build()).execute();
			if (response.isSuccessful()) {
				return response.body().string();
			}
			String message = response.message();
			if (StringUtils.isBlank(message)) {
				message = HttpStatus.valueOf(response.code()).getReasonPhrase();
			}
			if (logger.isDebugEnabled())
				logger.debug("call_remote_api_error ->url:{},code:{},message:{}", url, response.code(), message);
			throw new JeesuiteBaseException(response.code(), message);
		} catch (Exception e) {
			logger.error("调用远程服务失败[" + url + "]", e);
			throw new JeesuiteBaseException("系统繁忙，稍后再试[004]");
		}

	}
	
	public static String postJSON(String url, String json) {
		
		RequestBody requestBody = FormBody.create(JSON_MEDIA_TYPE, json);
		Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
		
		try {
			Response response = httpClient.newCall(request).execute();
			if (response.isSuccessful()) {
				return response.body().string();
			}
			String message = response.message();
			if (StringUtils.isBlank(message)) {
				message = HttpStatus.valueOf(response.code()).getReasonPhrase();
			}
			if (logger.isDebugEnabled())
				logger.debug("call_remote_api_error ->url:{},code:{},message:{}", url, response.code(), message);
			throw new JeesuiteBaseException(response.code(), message);
		} catch (Exception e) {
			logger.error("调用远程服务失败[" + url + "]", e);
			throw new JeesuiteBaseException("系统繁忙，稍后再试[004]");
		}

	}

	public static class Builder {
		private String requestUrl;
		private HttpMethod requestMethod;
		private MediaType mediaType;
		private Map<String, Object> requestParameters;
		private Map<String, String> headers;
		private Object requestData;
		private Class<?> responseClass = String.class;
		private TypeReference<?> responseTypeReference;

		public Builder requestUrl(String requestUrl) {
			this.requestUrl = requestUrl;
			return this;
		}

		public Builder requestMethod(HttpMethod httpMethod) {
			this.requestMethod = httpMethod;
			return this;
		}

		public Builder requestParameters(Map<String, Object> requestParameters) {
			this.requestParameters = requestParameters;
			return this;
		}

		public Builder addParameter(String name, String value) {
			if (StringUtils.isAnyBlank(name, value))
				return this;
			if (this.requestParameters == null)
				this.requestParameters = new HashMap<>();
			this.requestParameters.put(name, value);
			return this;
		}
		
		public Builder addHeader(String name, String value) {
			if (StringUtils.isAnyBlank(name, value))
				return this;
			if (this.headers == null)
				this.headers = new HashMap<>();
			this.headers.put(name, value);
			return this;
		}

		// new TypeReference<Page<QueryStaffResponse>>() {}
		public Builder responseTypeReference(TypeReference<?> responseTypeReference) {
			this.responseTypeReference = responseTypeReference;
			return this;
		}

		public Builder responseClass(Class<?> responseClass) {
			this.responseClass = responseClass;
			return this;
		}

		public Builder requestData(Object requestData) {
			this.requestData = requestData;
			return this;
		}

		public Builder formUrlEncoded() {
			this.mediaType = FROM_URLENCODED_MEDIA_TYPE;
			return this;
		}

		public GenericApiRequest build() {
			if (mediaType == null) {
				if (requestMethod == HttpMethod.GET || requestData == null) {
					mediaType = FROM_URLENCODED_MEDIA_TYPE;
				} else {
					mediaType = JSON_MEDIA_TYPE;
				}
			}

			if (requestMethod == null) {
				if (requestData != null) {
					requestMethod = HttpMethod.POST;
				} else {
					requestMethod = HttpMethod.GET;
				}
			}
			return new GenericApiRequest(requestUrl, requestMethod, mediaType, headers,requestParameters,
					requestData, responseClass, responseTypeReference);
		}
	}

}
