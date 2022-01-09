package com.jeesuite.zuul.filter.pre;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.DigestUtils;
import com.jeesuite.common.util.ParameterUtils;
import com.jeesuite.springweb.servlet.HttpServletRequestReader;
import com.jeesuite.zuul.filter.AbstractZuulFilter;
import com.jeesuite.zuul.filter.FilterHandler;
import com.jeesuite.zuul.model.BizSystemModule;
import com.netflix.zuul.context.RequestContext;


/**
 * 
 * 
 * <br>
 * Class Name   : SignatureRequestHandler
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date 2021-04-23
 */
public class SignatureRequestHandler implements FilterHandler{

	private static final String X_SIGN_HEADER = "x-sign";
	private static final String APP_ID_HEADER = "appId";
	private static final String TIMESTAMP_HEADER = "timestamp";
	
	private Map<String, String> appIdSecretMappings = new HashMap<String, String>();
	

	public SignatureRequestHandler(Map<String, String> configs) {
		this.appIdSecretMappings = configs;
	}

	@Override
	public Object process(RequestContext ctx, HttpServletRequest request, BizSystemModule module) {
		
		String sign = request.getHeader(X_SIGN_HEADER);
		if(StringUtils.isBlank(sign))return null;
		String timestamp = request.getHeader(TIMESTAMP_HEADER);
		String appId = request.getHeader(APP_ID_HEADER);
		
		if(StringUtils.isAnyBlank(timestamp,appId)) {
			throw new JeesuiteBaseException("认证头信息不完整");
		}
		
		String secret = appIdSecretMappings.get(appId);
		
		if(StringUtils.isBlank(secret)) {
			throw new JeesuiteBaseException("appId不存在");
		}
		
		Map<String, Object> requestDatas = new HttpServletRequestReader(request).getRequestDatas();
		
		String signBaseString = StringUtils.trimToEmpty(ParameterUtils.mapToQueryParams(requestDatas))  + timestamp + secret;
		String expectSign = DigestUtils.md5(signBaseString);
		
		if(!expectSign.equals(sign)) {
			throw new JeesuiteBaseException("签名错误");
		}
		
		ctx.set(AbstractZuulFilter.CTX_IGNORE_AUTH, Boolean.TRUE);
		
		return null;
	}

	@Override
	public int order() {
		return 0;
	}

}
