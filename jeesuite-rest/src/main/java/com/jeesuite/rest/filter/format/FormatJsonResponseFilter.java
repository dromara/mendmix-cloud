package com.jeesuite.rest.filter.format;

import java.io.IOException;

import javax.annotation.Priority;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.rest.RestConst;
import com.jeesuite.rest.response.ResponseCode;
import com.jeesuite.rest.response.RestResponse;

/**
 * rest响应过滤器，用于将返回值转为RestResponse
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年2月25日
 */
@Priority(10)
public class FormatJsonResponseFilter implements ContainerResponseFilter {

	private static Logger log = LoggerFactory.getLogger(FormatJsonResponseFilter.class);

	@Context
	private ResourceInfo resourceInfo;

	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {

		try {

			MediaType mediaType = responseContext.getMediaType();
			if (mediaType != null && !MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
				return;
			}

			Object responseData = responseContext.getEntity();
			// log.debug("responseData=" + responseData);

			RestResponse jsonResponse;

			if (responseData instanceof RestResponse) {
				jsonResponse = (RestResponse) responseData;
			} else {
				jsonResponse = new RestResponse(ResponseCode.成功);
				jsonResponse.setData(responseData);
			}
			// log.debug("restResponse=" + jsonResponse);

			responseContext.setStatus(ResponseCode.成功.getCode());

			responseContext.setEntity(jsonResponse);

			if(log.isDebugEnabled() && ResponseCode.找不到路径.getCode() != jsonResponse.getCode()){
				Object startTime = requestContext.getProperty(RestConst.PROP_REQUEST_BEGIN_TIME);
				long useTime = startTime != null ? System.currentTimeMillis() - (Long)startTime : 0;
				log.debug("======RestResponse======\n{}\nUse Time:{} ms\n",JsonUtils.toJson(jsonResponse),useTime);
			}

		} catch (Exception e) {
			throw new RuntimeException("转换响应统一格式发生异常", e);
		}

	}

}
