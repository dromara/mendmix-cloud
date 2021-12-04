package com.jeesuite.springweb.ext.servlet;

import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;

import com.jeesuite.common.GlobalConstants;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.DigestUtils;
import com.jeesuite.common.util.JsonUtils;
import com.jeesuite.common.util.ParameterUtils;
import com.jeesuite.common.util.WebUtils;

/**
 * 
 * <br>
 * Class Name   : HttpServletRequestReader
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年5月13日
 */
public class HttpServletRequestReader {

	private String requestUri;
	private boolean postMethod;
	private byte[] stream;
	private String queryString;
	private String streamAsString;
	private Map<String, Object> requestDatas;
	private boolean requestJson;
	
	private String cacheKey;
	/**
	 * @param request
	 */
	public HttpServletRequestReader(HttpServletRequest request) {
		try {	
			requestUri = request.getRequestURI();
			queryString = request.getQueryString();
			this.postMethod = HttpMethod.POST.equals(request.getMethod());
			this.requestDatas =  queryParamsToMap(request);
			if(!WebUtils.isMultipartContent(request)){	
				this.stream = IOUtils.toByteArray(request.getInputStream());
				if(this.stream != null){
					streamAsString = new String(this.stream,StandardCharsets.UTF_8.name());
					parseStreamContentToMap(streamAsString);
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String getRequestUri() {
		return requestUri;
	}

	public boolean isPostMethod() {
		return postMethod;
	}

	public byte[] getStream() {
		return stream;
	}

	public String getQueryString() {
		return queryString;
	}

	public String getStreamAsString() {
		return streamAsString;
	}

	public Map<String, Object> getRequestDatas() {
		return requestDatas;
	}

	public boolean isRequestJson() {
		return requestJson;
	}
	
	public String getCacheKey() {
		if(cacheKey == null){
			StringBuilder builder = new StringBuilder();
			builder.append(postMethod).append(requestUri);
			if(requestDatas != null && !requestDatas.isEmpty()){
				String paramsString = ParameterUtils.mapToQueryParams(requestDatas);
				if(paramsString.length() > 32)paramsString = DigestUtils.md5(paramsString);
				builder.append(GlobalConstants.COLON).append(paramsString);
			}
			cacheKey = builder.toString();
		}
		return cacheKey;
	}

	public String getParamValue(String ...paramNames){
		String value = null;
		for (String name : paramNames) {
			value = Objects.toString(requestDatas.get(name), null);
			if(value != null)return value;
		}
		throw new JeesuiteBaseException();
	}
	
	private Map<String, Object> queryParamsToMap(HttpServletRequest request) {

		Map<String, Object> params = new HashMap<String, Object>();
		Enumeration<String> e = request.getParameterNames();

		StringBuilder tmpbuff = new StringBuilder();
		if (e.hasMoreElements()) {
			while (e.hasMoreElements()) {
				String name = e.nextElement();
				String[] values = request.getParameterValues(name);
				if (values.length == 1) {
					if (StringUtils.isNotBlank(values[0]))
						params.put(name, values[0]);
				} else {
					tmpbuff.setLength(0);
					for (int i = 0; i < values.length; i++) {
						if (StringUtils.isNotBlank(values[i])) {
							tmpbuff.append(values[i].trim()).append(",");
						}
					}
					if (tmpbuff.length() > 0) {
						tmpbuff.deleteCharAt(tmpbuff.length() - 1);
						params.put(name, tmpbuff.toString());
					}
				}
			}
		}
		
		return params;
	}
	
	private void parseStreamContentToMap(String streamContent){
		try {
			Map map = JsonUtils.toObject(streamContent, Map.class);
			requestDatas.putAll(map);
			requestJson = true;
		} catch (Exception e) {
			String[] split = StringUtils.splitByWholeSeparator(streamContent, ParameterUtils.CONTACT_STR);
			for (String s : split) {
				String[] split2 = StringUtils.splitByWholeSeparator(s, ParameterUtils.EQUALS_STR);
				if (split2.length == 2 && StringUtils.isNotBlank(split2[1])) {
					requestDatas.put(split2[0], split2[1]);
				}
			}
		}
	}
}
