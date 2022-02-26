package com.jeesuite.gateway.zuul.filter.pre;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.CurrentRuntimeContext;
import com.jeesuite.common.CustomRequestHeaders;
import com.jeesuite.common.model.AuthUser;
import com.jeesuite.common.util.SimpleCryptUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.gateway.model.BizSystemModule;
import com.jeesuite.gateway.zuul.filter.FilterHandler;
import com.jeesuite.springweb.client.RequestHeaderBuilder;
import com.netflix.zuul.context.RequestContext;


/**
 * 
 * 
 * <br>
 * Class Name   : GlobalHeaderHanlder
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2020年9月15日
 */
public class GlobalHeaderHandler implements FilterHandler {

	@Override
	public Object process(RequestContext ctx, HttpServletRequest request, BizSystemModule module) {
		String requrstId = request.getHeader(CustomRequestHeaders.HEADER_REQUEST_ID);
		if (StringUtils.isNotBlank(requrstId)) {
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_REQUEST_ID, requrstId);
		} else {
			ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_REQUEST_ID, TokenGenerator.generate());
		}
		
		ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_INVOKER_IS_GATEWAY, Boolean.TRUE.toString());

		RequestHeaderBuilder.getHeaders().forEach((k, v) -> {
			ctx.addZuulRequestHeader(k, v);
		});

		// 跨集群
		boolean crossCluster = false;
		try {
			String clusterName = request.getHeader(CustomRequestHeaders.HEADER_CLUSTER_ID);
			if (clusterName != null) {
				clusterName = SimpleCryptUtils.decrypt(clusterName);
			}
			// TODO 验证合法性
			crossCluster = true;
		} catch (Exception e) {
		}

		if (!crossCluster) {
			// 一些header禁止前端传入
			if (request.getHeader(CustomRequestHeaders.HEADER_IGNORE_TENANT) != null) {
				ctx.getZuulRequestHeaders().remove(CustomRequestHeaders.HEADER_IGNORE_TENANT);
			}
			if (request.getHeader(CustomRequestHeaders.HEADER_AUTH_USER) != null) {
				ctx.getZuulRequestHeaders().remove(CustomRequestHeaders.HEADER_AUTH_USER);
				AuthUser currentUser = CurrentRuntimeContext.getCurrentUser();
				if (currentUser != null) {
					ctx.addZuulRequestHeader(CustomRequestHeaders.HEADER_AUTH_USER, currentUser.toEncodeString());
				}
			}
		}

		return null;
	}

	@Override
	public int order() {
		return 1;
	}

}
