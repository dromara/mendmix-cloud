/**
 * 
 */
package com.jeesuite.rest.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.jeesuite.rest.RestConst;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年1月21日
 * @Copyright (c) 2015, vakinge@github
 */
public class RequestUtils {

	/**
	 *  获取请求参数
	 * @param request
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	public static Map<String, Object> getParametersMap(ContainerRequestContext requestContext,HttpServletRequest request)
			throws IOException {
		
		if(isMultipartContent(request)){
			return buildQueryParamsMap(request);
		}
		
		Map<String, Object> parameters = buildQueryParamsMap(request);
		
		if (RestConst.GET_METHOD.equals(request.getMethod())) {
			return parameters;
		}else if (RestConst.POST_METHOD.equals(request.getMethod())) {
			
			byte[] byteArray = IOUtils.toByteArray(requestContext.getEntityStream());
			//reset InputStream
			requestContext.setEntityStream(new ByteArrayInputStream(byteArray));
			
			if(byteArray == null || byteArray.length == 0)return parameters;
			String content = new String(byteArray);
			//JSON 
//			try {
//				return JsonUtils.toObject(content, Map.class);
//			} catch (Exception e) {}
			if(content.contains("{")){
				content = StringUtils.left(content, 2048).trim();
				if(!content.endsWith("}"))content = content + "...\n}";
				parameters.put("data", content);
				return parameters;
			}
			
			String[] split = content.split("\\&");
			for (String s : split) {
				String[] split2 = s.split("=");
				if (split2.length == 2 && StringUtils.isNotBlank(split2[1])) {
					parameters.put(split2[0], split2[1]);
				}
			}
			return parameters;
		
		}
		return null;
	}


	/**
	 *   是否multipart/form-data or application/octet-stream表单提交方式
	 * @param request
	 * @return
	 */
	public static final boolean isMultipartContent(HttpServletRequest request) {
		if (!RestConst.POST_METHOD.equalsIgnoreCase(request.getMethod())) {
			return false;
		}
		String contentType = request.getContentType();
		if (contentType == null) {
			return false;
		}
		contentType = contentType.toLowerCase(Locale.ENGLISH);
		if (contentType.startsWith(RestConst.MULTIPART) 
				|| MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
			return true;
		}
		return false;
	}
	
	protected static Map<String, Object> buildQueryParamsMap(HttpServletRequest request) {

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

}
