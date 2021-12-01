package com.jeesuite.zuul.support;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.security.AuthAdditionHandler;
import com.jeesuite.security.model.UserSession;
import com.netflix.zuul.context.RequestContext;

public class ZuulGatewayAuthAdditionHandler implements AuthAdditionHandler {

	@Override
	public void beforeAuthorization(HttpServletRequest request, HttpServletResponse response) {
		String clientType = request.getHeader(CustomRequestHeaders.HEADER_CLIENT_TYPE);
		if(clientType != null) {
			CurrentRuntimeContext.setClientType(clientType);
			RequestContext.getCurrentContext().addZuulRequestHeader(CustomRequestHeaders.HEADER_CLIENT_TYPE, clientType);
		}
	}

	@Override
	public void afterAuthorization(UserSession userSession) {
		RequestContext ctx = RequestContext.getCurrentContext();
		if(userSession != null && !userSession.isAnonymous()) {
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_AUTH_USER, userSession.getUser().toEncodeString());
		}
		String tenantId = userSession == null ? null : userSession.getTenantId();
		if(tenantId != null) {
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_TENANT_ID, tenantId);
		}
	}

}
