package com.jeesuite.gateway.zuul.error;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.web.bind.annotation.RequestMapping;

import com.jeesuite.common.model.WrapperResponse;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;

public class ErrorHandlerController implements ErrorController {

	private static Logger log = LoggerFactory.getLogger("com.jeesuite");

	public String getErrorPath() {
		return "/error";
	}

	@RequestMapping("/error")
	public WrapperResponse<Void> error(HttpServletRequest request, HttpServletResponse response) {
		RequestContext ctx = RequestContext.getCurrentContext();
		ZuulException zuulException = (ZuulException) ctx.getThrowable();
		Throwable ex = zuulException;
		while (ex.getCause() != null && ex.getCause().getClass() != ex.getClass()) {
			ex = ex.getCause();
		}

		String errorMessage = ex.getMessage();
		int errorCode = ctx.getResponseStatusCode();

		log.error("gateway_global_exception -> errorCode:{},errorMessage:{}", errorCode, errorMessage);

		WrapperResponse<Void> errorResponse = new WrapperResponse<>();

		String bizErrorCode = "error.global.system.error";
		if (ex instanceof java.net.ConnectException) {
			bizErrorCode = "error.request.connectFail";
		} else if (ex instanceof java.net.SocketTimeoutException) {
			errorCode = 504;
			bizErrorCode = "error.request.timeout";
		} else if (ex instanceof com.netflix.client.ClientException) {
			errorCode = 503;
			bizErrorCode = "error.service.unavailable";
		}

		errorResponse.setCode(errorCode);
		errorResponse.setMsg(bizErrorCode);

		return errorResponse;
	}

}
