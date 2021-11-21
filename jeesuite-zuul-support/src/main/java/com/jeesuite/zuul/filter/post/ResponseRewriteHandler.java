package com.jeesuite.zuul.filter.post;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.io.CharStreams;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.zuul.filter.FilterHandler;
import com.jeesuite.zuul.model.BizSystemModule;
import com.netflix.util.Pair;
import com.netflix.zuul.context.RequestContext;

public class ResponseRewriteHandler implements FilterHandler {
	
	private static Logger log = LoggerFactory.getLogger("com.jeesuite.zuul.filter");
	
	private static final String DEFAULT_ERROR_MSG = "系统繁忙";
	private static final String _MESSAGE_NAME = "message";
	private static final String _MSG_NAME = "msg";
	private static final String _CODE_NAME = "code";
	private static final String _DATA_NAME = "data";
	
	@Override
	public Object process(RequestContext ctx, HttpServletRequest request, BizSystemModule module) {
		
		int statusCode = ctx.getResponseStatusCode();
		if(statusCode != 200)return null;
		
		InputStream responseDataStream = ctx.getResponseDataStream();
		if(responseDataStream == null)return null;
		
		List<Pair<String, String>> headers = ctx.getOriginResponseHeaders();
		for (Pair<String, String> pair : headers) {
			if (CustomRequestHeaders.HEADER_RESP_KEEP.equals(pair.first())) {
				if (Boolean.parseBoolean(pair.second())) {
					return null;
				}
			}
			//
			if (HttpHeaders.CONTENT_TYPE.equalsIgnoreCase(pair.first())
					&& !pair.second().contains(MediaType.APPLICATION_JSON_VALUE)
					&& !pair.second().contains(MediaType.TEXT_PLAIN_VALUE)
					&& !pair.second().contains(MediaType.TEXT_HTML_VALUE)) {
				return null;
			}

			if (HttpHeaders.CONTENT_DISPOSITION.equalsIgnoreCase(pair.first())) {
				return null;
			}
		}
		
		//
		String responseMsg = null;
		String responseData = null;
		JSONObject originRespJSON = null;
		boolean rebuild = Boolean.FALSE;
		try {
			responseData = responseDataStream == null ? null
					: CharStreams.toString(new InputStreamReader(responseDataStream, StandardCharsets.UTF_8));
			if (StringUtils.isBlank(responseData)) {
				rebuild = true;
				return null;
			}

			if (log.isTraceEnabled()) {
				log.trace("ORIGIN_RESPONSE -> {}", responseData);
			}

			try {
				originRespJSON = JSON.parseObject(responseData);
			} catch (Exception e) {
			}
			// 已经包含code结构不处理
			if (originRespJSON == null || !(originRespJSON.containsKey(_CODE_NAME) && (originRespJSON.containsKey(_DATA_NAME) || originRespJSON.containsKey(_MSG_NAME)))) {
				rebuild = true;
				if (statusCode != 200) {
					try {
						responseMsg = originRespJSON.getString(_MESSAGE_NAME);
					} catch (Exception e) {
					}
					if (responseMsg == null) {
						try {
							responseMsg = HttpStatus.valueOf(statusCode).getReasonPhrase();
						} catch (Exception e) {
						}
					}
					if (responseMsg == null)
						responseMsg = DEFAULT_ERROR_MSG;
				}
			}

		} catch (Exception e) {
			String error = "Error during filtering[ResponseFilter]";
			log.error(error, e);
			statusCode = 500;
			responseMsg = DEFAULT_ERROR_MSG;
		} finally {
			// 系统异常全部正常返回
			if (statusCode == 500) {
				ctx.setResponseStatusCode(200);
			}
			//
			if (rebuild) {
				JSONObject respJSONObject = new JSONObject();
				respJSONObject.put(_CODE_NAME, statusCode == 200 ? 200 : 500);
				if (StringUtils.isNotBlank(responseMsg))
					respJSONObject.put(_MSG_NAME, responseMsg);
				if (originRespJSON != null) {
					respJSONObject.put(_DATA_NAME, originRespJSON);
				} else if (StringUtils.isNotBlank(responseData)) {
					try {
						respJSONObject.put(_DATA_NAME, JSON.parse(responseData));
					} catch (Exception e2) {
						respJSONObject.put(_DATA_NAME, responseData);
					}
				}

				responseData = respJSONObject.toJSONString();
				ctx.setResponseBody(responseData);
			}else{
				ctx.setResponseBody(responseData);
			}
		}
		
		return null;
	}

	@Override
	public int order() {
		return 0;
	}

}
