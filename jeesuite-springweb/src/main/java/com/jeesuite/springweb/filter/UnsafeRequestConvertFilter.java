package com.jeesuite.springweb.filter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;

import com.google.common.io.CharStreams;
import com.jeesuite.springweb.base.CustomHttpServletRequestWrapper;
import com.jeesuite.springweb.utils.UnsafeCharCheckUtils;
import com.jeesuite.springweb.utils.WebUtils;

public class UnsafeRequestConvertFilter implements Filter {

	private List<String> ignoreSuffixs = new ArrayList<>(Arrays.asList(".html",".htm",".css",".js",".jpg",".png",".gif",".ttf"));
	
	private String[] ignorePaths = new String[]{"/webjars/","/swagger"};
	@Override
	public void init(FilterConfig filterConfig) throws ServletException {}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		
		HttpServletRequest httpReq = (HttpServletRequest) request;
		
		//包含后缀的不处理
		boolean doWrap = httpReq.getRequestURI().contains(".") == false;
		
//		String suffix = httpReq.getRequestURI().toLowerCase().substring(httpReq.getRequestURI().lastIndexOf("."));
//		doWrap = ignoreSuffixs.contains(suffix) == false;
		if(doWrap){
			for (String path : ignorePaths) {
				if(httpReq.getRequestURI().contains(path)){
					doWrap = false;
					break;
				}
			}
		}

		if(doWrap){
			//如果是上传则忽略
			doWrap = WebUtils.isMultipartContent(httpReq) == false;
		}
		
		if(doWrap){		
			String content = CharStreams.toString(new InputStreamReader(request.getInputStream(), StandardCharsets.UTF_8.name()));
			content = UnsafeCharCheckUtils.replaceSpecChars(content);
			byte[] stream = StringUtils.isBlank(content) ? new byte[0] : content.getBytes(StandardCharsets.UTF_8.name()); 
			CustomHttpServletRequestWrapper requestWrapper = new CustomHttpServletRequestWrapper(httpReq,stream);  
			chain.doFilter(requestWrapper , response);  
		}else{
			chain.doFilter(request , response);  
		}
	}

	@Override
	public void destroy() {}

}
