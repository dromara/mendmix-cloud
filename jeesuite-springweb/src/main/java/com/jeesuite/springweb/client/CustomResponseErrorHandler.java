package com.jeesuite.springweb.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.google.common.io.CharStreams;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.springweb.model.WrapperResponseEntity;

public class CustomResponseErrorHandler extends DefaultResponseErrorHandler {
	/**
	 * 
	 */
	private static final String DEFAULT_ERROR_MSG = "invoke remote service error";

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		int code = response.getRawStatusCode();
		String content = CharStreams.toString(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));

		WrapperResponseEntity entity = null;
		Map<?, ?> responseItmes = null;
		try {
			if(content.contains("code")){				
				entity = JsonUtils.toObject(content, WrapperResponseEntity.class);
			}else{
				responseItmes = JsonUtils.toObject(content, Map.class);
				String errorMsg = DEFAULT_ERROR_MSG;
				if(responseItmes != null && responseItmes.containsKey("message")){
					errorMsg = responseItmes.get("message").toString();
				}
				entity = new WrapperResponseEntity(500, errorMsg);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (entity != null) {
			throw new JeesuiteBaseException(entity.getCode(), entity.getMsg());
		} else {
			throw new JeesuiteBaseException(code, DEFAULT_ERROR_MSG);
		}

	}

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return response.getRawStatusCode() != 200;
	}
}
