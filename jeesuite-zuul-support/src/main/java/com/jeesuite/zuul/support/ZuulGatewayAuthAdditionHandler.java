package com.jeesuite.zuul.support;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.jeesuite.common.WebConstants;
import com.jeesuite.security.AuthAdditionHandler;
import com.jeesuite.security.model.UserSession;
import com.netflix.zuul.context.RequestContext;

public class ZuulGatewayAuthAdditionHandler implements AuthAdditionHandler {

	@Override
	public void beforeAuthorization(HttpServletRequest request, HttpServletResponse response) {}

	@Override
	public void afterAuthorization(UserSession userSession) {
		if(userSession != null && !userSession.isAnonymous()) {
			RequestContext ctx = RequestContext.getCurrentContext();
			ctx.addZuulRequestHeader(WebConstants.HEADER_AUTH_USER, userSession.getUser().toEncodeString());
			if(userSession.getTenantId() != null) {				
				ctx.addZuulRequestHeader(WebConstants.HEADER_TENANT_ID, userSession.getTenantId());
			}
		}
	}

}
