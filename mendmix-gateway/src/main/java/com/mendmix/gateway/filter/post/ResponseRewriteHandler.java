/*
 * Copyright 2016-2020 www.mendmix.com.
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
package com.mendmix.gateway.filter.post;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;

import com.mendmix.common.CustomRequestHeaders;
import com.mendmix.common.model.WrapperResponse;
import com.mendmix.common.util.JsonUtils;
import com.mendmix.gateway.filter.PostFilterHandler;
import com.mendmix.gateway.model.BizSystemModule;

/**
 * 
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2020年4月16日
 */
public class ResponseRewriteHandler implements PostFilterHandler {
	
	private static Logger logger = LoggerFactory.getLogger("com.mendmix.gateway");
	
	private static final String DEFAULT_ERROR_MSG = "系统繁忙";
	private static final String _MESSAGE_NAME = "message";
	private static final String _MSG_NAME = "msg";
	private static final String _CODE_NAME = "code";
	private static final String _DATA_NAME = "data";
	
	@Override
	public String process(ServerWebExchange exchange, BizSystemModule module,String respBodyAsString) {
		
		if(!exchange.getResponse().getStatusCode().is2xxSuccessful()) {
			return respBodyAsString;
		}
		
		if(exchange.getRequest().getHeaders().containsKey(CustomRequestHeaders.HEADER_RESP_KEEP)) {
			return respBodyAsString;
		}
		
		if(exchange.getResponse().getHeaders().containsKey(CustomRequestHeaders.HEADER_RESP_KEEP)) {
			return respBodyAsString;
		}
		//
		if(StringUtils.isBlank(respBodyAsString)) {
			return JsonUtils.toJson(WrapperResponse.success());
		}
		
		int statusCode = exchange.getResponse().getStatusCode().value();
		
		
		String responseData = respBodyAsString;
		String responseMsg = null;
		Map<String, Object> originObject = null;
        boolean rebuild = false;
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("ORIGIN_RESPONSE -> {}", responseData);
			}

			try {
				originObject = JsonUtils.toHashMap(responseData);
			} catch (Exception e) {
			}
			// 已经包含code结构不处理
			boolean isCodeResp = originObject != null
								&& originObject.containsKey(_CODE_NAME)
							    && (originObject.size() == 1 || originObject.containsKey(_DATA_NAME) || originObject.containsKey(_MSG_NAME));
			if (!isCodeResp) {
				rebuild = true;
				if (statusCode != 200) {
					try {
						responseMsg = Objects.toString(originObject.get(_MESSAGE_NAME), null);
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
			logger.error(error, e);
			statusCode = 500;
			responseMsg = DEFAULT_ERROR_MSG;
		} finally {
			// 系统异常全部正常返回
			if (statusCode == 500) {
				exchange.getResponse().setStatusCode(HttpStatus.OK);
			}
			//
			if (rebuild) {
				Map<String, Object> rebuildObject = new LinkedHashMap<>(3);
				rebuildObject.put(_CODE_NAME, statusCode == 200 ? 200 : 500);
				if (StringUtils.isNotBlank(responseMsg))
					rebuildObject.put(_MSG_NAME, responseMsg);
				if (originObject != null) {
					rebuildObject.put(_DATA_NAME, originObject);
				} else if (StringUtils.isNotBlank(responseData)) {
					rebuildObject.put(_DATA_NAME, originObject);
				}
				responseData = JsonUtils.toJson(rebuildObject);
			}
		}
		
		return responseData;
	}

	@Override
	public int order() {
		return 1;
	}
	

}
