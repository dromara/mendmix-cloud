package com.jeesuite.springweb.client;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

import com.google.common.io.CharStreams;
import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.springweb.model.WrapperResponseEntity;

public class CustomResponseErrorHandler extends DefaultResponseErrorHandler {
	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		int code = response.getRawStatusCode();
		String content = CharStreams.toString(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
		
		WrapperResponseEntity entity = null;
		try {
			entity = JsonUtils.toObject(content, WrapperResponseEntity.class);
		} catch (Exception e) {}
		
		if(entity != null){
			throw new JeesuiteBaseException(entity.getCode(), entity.getMsg());
		}else{
			throw new JeesuiteBaseException(code, "invoke remote service error");
		}

	}

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return response.getRawStatusCode() != 200;
	}
}
