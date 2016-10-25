package com.jeesuite.rest.excetion;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.NotSupportedException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.jeesuite.rest.response.ResponseCode;
import com.jeesuite.rest.response.RestResponse;

/**
 * REST 异常映射
 * 
 * 将异常转换成RestResponse
 * 
 * @author LinHaobin
 *
 */
public class BaseExceptionMapper implements ExceptionMapper<Exception> {

	private static Logger log = LoggerFactory.getLogger(BaseExceptionMapper.class);
	private static final String DEFAULT_ERROR_MSG = "系统繁忙,请稍后再试!";

	@Context
	private HttpServletRequest request;
	
	@Override
	public Response toResponse(Exception e) {

		RestResponse response = null;
		if (e instanceof NotFoundException) {
			response = new RestResponse(ResponseCode.找不到路径);
		} else if (e instanceof NotAllowedException) {
			response = new RestResponse(ResponseCode.禁止访问);
		} else if (e instanceof JsonProcessingException) {
			response = new RestResponse(ResponseCode.错误JSON);
		} else if (e instanceof NotSupportedException) {
			response = new RestResponse(ResponseCode.不支持的媒体类型);
		} else {
			response = new RestResponse(ResponseCode.服务器异常);
		}
		return Response.status(response.getCode()).type(MediaType.APPLICATION_JSON).entity(response).build();
	}

}
