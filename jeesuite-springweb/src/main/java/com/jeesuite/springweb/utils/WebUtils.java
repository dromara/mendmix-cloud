package com.jeesuite.springweb.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.springweb.RequestContextHelper;
import com.jeesuite.springweb.WebConstants;

public class WebUtils {

	private static final String POINT = ".";
	private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
	private static final String MULTIPART = "multipart/";
	
	public static boolean isAjax(HttpServletRequest request){
	    return  (request.getHeader(WebConstants.HEADER_REQUESTED_WITH) != null  
	    && XML_HTTP_REQUEST.equalsIgnoreCase(request.getHeader(WebConstants.HEADER_REQUESTED_WITH).toString())) ;
	}
	
	public static  void responseOutJson(HttpServletResponse response,String json) {  
	    //将实体对象转换为JSON Object转换  
	    response.setCharacterEncoding("UTF-8");  
	    response.setContentType("application/json; charset=utf-8");  
	    PrintWriter out = null;  
	    try {  
	        out = response.getWriter();  
	        out.append(json);  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    } finally {  
	        if (out != null) {  
	            out.close();  
	        }  
	    }  
	}  
	
	public static  void responseOutHtml(HttpServletResponse response,String html) {  
	    //将实体对象转换为JSON Object转换  
	    response.setCharacterEncoding("UTF-8");  
	    response.setContentType("text/html; charset=utf-8");  
	    PrintWriter out = null;  
	    try {  
	        out = response.getWriter();  
	        out.append(html);  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    } finally {  
	        if (out != null) {  
	            out.close();  
	        }  
	    }  
	}  
	
	public static  void responseOutJsonp(HttpServletResponse response,String callbackFunName,Object jsonObject) {  
	    //将实体对象转换为JSON Object转换  
	    response.setCharacterEncoding("UTF-8");  
	    response.setContentType("text/plain; charset=utf-8");  
	    PrintWriter out = null;  
	    
	    String json = (jsonObject instanceof String) ? jsonObject.toString() : JsonUtils.toJson(jsonObject);
	    String content = callbackFunName + "("+json+")";
	    try {  
	        out = response.getWriter();  
	        out.append(content);  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    } finally {  
	        if (out != null) {  
	            out.close();  
	        }  
	    }  
	} 

	
	/**
	 * 获取根域名
	 * @param request
	 * @return
	 */
	public static  String getRootDomain(HttpServletRequest request) {
		String host = request.getHeader(WebConstants.HEADER_FORWARDED_HOST);
		if(StringUtils.isBlank(host))host = request.getServerName();
		
		if(IpUtils.isIp(host) || IpUtils.LOCAL_HOST.equals(host)){
			return host;
		}
		
		String[] segs = StringUtils.split(host, POINT);
		int len = segs.length;
		return segs[len - 2] + POINT+ segs[len - 1];
	}
	
	public static  String getRootDomain(String url) {
		String host = getDomain(url);
		if(IpUtils.isIp(host) || IpUtils.LOCAL_HOST.equals(host))return host;
		
		String[] segs = StringUtils.split(host, POINT);
		int len = segs.length;
		return segs[len - 2] + POINT+ segs[len - 1];
	}
	
	public static  String getDomain(String url) {
		String[] urlSegs = StringUtils.split(url,"/");
		return urlSegs[1];
	}
	
	public static  String getBaseUrl(String url) {
		String[] segs = StringUtils.split(url,"/");
		return segs[0] + "//" + segs[1];
	}
	
	/**
	 * 获取baseurl<br>
	 * nginx转发需设置 proxy_set_header   X-Forwarded-Proto $scheme;
	 * @param request
	 * @return
	 */
	public static String getBaseUrl(HttpServletRequest request){
        String baseUrl = null;					
		String host = request.getHeader(WebConstants.HEADER_FORWARDED_HOST);
		String prefix = request.getHeader(WebConstants.HEADER_FORWARDED_PRIFIX);
		if(StringUtils.isAnyBlank(host,prefix)){
			String[] segs = StringUtils.split(request.getRequestURL().toString(),"/");
			baseUrl = segs[0] + "//" + segs[1];
		}else{
			//由于nginx 没有设置  proxy_set_header   X-Forwarded-Proto $scheme;
		    //导致https通过nginx转发后，在api网关获取到的scheme为：http,
			//String proto = request.getHeader(BaseConstants.HEADER_FORWARDED_ORIGN_PROTO);
			//if(proto == null)proto = request.getHeader(BaseConstants.HEADER_FORWARDED_PROTO);
			 String port = request.getHeader(WebConstants.HEADER_FORWARDED_PORT);
			String schame = "443".equals(port) ? "https://" : "http://";
			baseUrl = schame + host + prefix;
		}
		
		return baseUrl;
	}
	
	public static Map<String, String> getCustomHeaders(){
		Map<String, String> headers = new HashMap<>();
		 HttpServletRequest request = RequestContextHelper.getRequest();
		Enumeration<String> headerNames = request.getHeaderNames();
		 while(headerNames.hasMoreElements()){
			 String headerName = headerNames.nextElement().toLowerCase();
			 if(headerName.startsWith(WebConstants.HEADER_PREFIX)){				 
				 String headerValue = request.getHeader(headerName);
				 if(headerValue != null)headers.put(headerName, headerValue);
			 }
		 }
		 //
		 headers.put(WebConstants.HEADER_INVOKER_IP, IpUtils.getLocalIpAddr());
		 
		 if(!headers.containsKey(WebConstants.HEADER_AUTH_TOKEN)){			 
			 headers.put(WebConstants.HEADER_AUTH_TOKEN, TokenGenerator.generateWithSign());
		 }
		 
		 return headers;
	}
	
	
	public static final boolean isMultipartContent(HttpServletRequest request) {
		if (!HttpMethod.POST.name().equalsIgnoreCase(request.getMethod())) {
			return false;
		}
		String contentType = request.getContentType();
		if (contentType == null) {
			return false;
		}
		contentType = contentType.toLowerCase(Locale.ENGLISH);
		if (contentType.startsWith(MULTIPART) 
				|| MediaType.APPLICATION_OCTET_STREAM.equals(contentType)) {
			return true;
		}
		return false;
	}
	
	/**
	 * 是否内网调用
	 * @param request
	 * @return
	 */
	public static boolean isInternalRequest(HttpServletRequest request){
		//
		if(Boolean.parseBoolean(request.getHeader(WebConstants.HEADER_INTERNAL_REQUEST))){
			return true;
		}

		boolean isInner = IpUtils.isInnerIp(request.getServerName());
		String forwardHost = request.getHeader(WebConstants.HEADER_FORWARDED_HOST);
		//来源于网关
		if(StringUtils.isNotBlank(forwardHost)){
			isInner = IpUtils.isInnerIp(StringUtils.split(forwardHost, ":")[0]);
		}else{			
			if(!isInner){
				isInner = IpUtils.isInnerIp(IpUtils.getinvokerIpAddr(request));
			}
		}
		return isInner;
	}
}
