package com.jeesuite.springweb.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.google.common.io.CharStreams;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.JsonUtils;

/**
 * 全局错误处理hanlder
 * 
 * <br>
 * Class Name   : CustomResponseErrorHandler
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2018年10月17日
 */
public class CustomResponseErrorHandler extends DefaultResponseErrorHandler {
	/**
	 * 
	 */
	private static final String DEFAULT_ERROR_MSG = "invoke remote service error";

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		int code = response.getRawStatusCode();
		String content = CharStreams.toString(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
		
		Map<?, ?> responseItmes = null;
		if(code == 404 && StringUtils.isNotBlank(content)){
			responseItmes = JsonUtils.toObject(content, Map.class);
			throw new JeesuiteBaseException(404, "Page Not Found["+responseItmes.get("path")+"]");
		}

		int errorCode = 500;
		String errorMsg = content;
		try {responseItmes = JsonUtils.toObject(content, Map.class);} catch (Exception e) {}
		if(responseItmes != null){
			if(responseItmes.containsKey("code")){
				errorCode = Integer.parseInt(responseItmes.get("code").toString());
			}
			if(responseItmes.containsKey("msg")){
				errorMsg = responseItmes.get("msg").toString();
			}else if(responseItmes.containsKey("message")){
				errorMsg = responseItmes.get("message").toString();
			}
		}
		
		if(StringUtils.isBlank(errorMsg)){
			errorMsg = DEFAULT_ERROR_MSG;
		}
		
		throw new JeesuiteBaseException(errorCode, errorMsg + "(Remote)");
	}

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return response.getRawStatusCode() != 200;
	}
}
