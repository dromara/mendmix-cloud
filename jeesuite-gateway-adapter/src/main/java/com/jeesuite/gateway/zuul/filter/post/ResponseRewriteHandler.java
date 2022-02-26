package com.jeesuite.gateway.zuul.filter.post;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.gateway.zuul.filter.FilterHandler;
import com.netflix.zuul.context.RequestContext;

public class ResponseRewriteHandler implements FilterHandler {
	
	private static Logger log = LoggerFactory.getLogger("com.jeesuite.gateway.filter");
	
	private static final String DEFAULT_ERROR_MSG = "系统繁忙";
	private static final String _MESSAGE_NAME = "message";
	private static final String _MSG_NAME = "msg";
	private static final String _CODE_NAME = "code";
	private static final String _DATA_NAME = "data";
	
	@Override
	public Object process(RequestContext ctx, HttpServletRequest request, BizSystemModule module) {
		
		ResponseCompose responseCompose = (ResponseCompose) ctx.get(ResponseCompose.class.getName());
		if(responseCompose == null) {
			responseCompose = new ResponseCompose(ctx);
		}
        
		if(!responseCompose.isSuccessed() || !responseCompose.isRewriteEnabled()) {
			return null;
		}

		//
		String responseMsg = null;
		String responseData = responseCompose.getBodyString();
		JSONObject originRespJSON = null;
		boolean rebuild = Boolean.FALSE;
		int statusCode = responseCompose.getStatusCode();
		try {
			if (StringUtils.isBlank(responseData)) {
				rebuild = true;
				return null;
			}
			originRespJSON = JSON.parseObject(responseData);
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
		return 1;
	}

}
