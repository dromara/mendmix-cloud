package com.jeesuite.springweb.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common.util.ResourceUtils;
import com.jeesuite.common.util.TokenGenerator;
import com.jeesuite.springweb.CurrentRuntimeContext;
import com.jeesuite.springweb.WebConstants;

public class WebUtils {

	private static final String POINT = ".";
	private static final String XML_HTTP_REQUEST = "XMLHttpRequest";
	private static final String MULTIPART = "multipart/";
	private static List<String> doubleDomainSuffixs = Arrays.asList(".com.cn",".org.cn",".net.cn");
	//内网解析域名
	private static List<String> internalDomains  = ResourceUtils.getList("internal.dns.domains");
	
	
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
		return parseHostRootDomain(host);
	}
	
	public static  String getRootDomain(String url) {
		String host = getDomain(url);
		return parseHostRootDomain(host);
	}
	
	private static String parseHostRootDomain(String host){
		if(IpUtils.isIp(host) || IpUtils.LOCAL_HOST.equals(host)){
			return host;
		}
		String[] segs = StringUtils.split(host, POINT);
		int len = segs.length;
		
		if(doubleDomainSuffixs.stream().anyMatch(e -> host.endsWith(e))){
			return segs[len - 3] + POINT + segs[len - 2] + POINT + segs[len - 1];
		}
		return segs[len - 2] + POINT + segs[len - 1];
	}
	
	public static  String getDomain(String url) {
		String[] urlSegs = StringUtils.split(url,"/");
		return urlSegs[1];
	}
	
	public static  String getBaseUrl(String url) {
		String[] segs = StringUtils.split(url,"/");
		return segs[0] + "//" + segs[1];
	}
	
	public static String getBaseUrl(HttpServletRequest request){
		return getBaseUrl(request, true);
	}
	
	/**
	 * 获取baseurl<br>
	 * nginx转发需设置 proxy_set_header   X-Forwarded-Proto $scheme;
	 * @param request
	 * @param withContextPath
	 * @return
	 */
	public static String getBaseUrl(HttpServletRequest request,boolean withContextPath){
        String baseUrl = null;
        
        String schame = request.getHeader(WebConstants.HEADER_FORWARDED_PROTO);
		String host = request.getHeader(WebConstants.HEADER_FORWARDED_HOST);
		String prefix = request.getHeader(WebConstants.HEADER_FORWARDED_PRIFIX);
		if(StringUtils.isBlank(host)){
			String[] segs = StringUtils.split(request.getRequestURL().toString(),"/");
			baseUrl = segs[0] + "//" + segs[1];
		}else{
			if(StringUtils.isBlank(schame)){
				String port = request.getHeader(WebConstants.HEADER_FORWARDED_PORT);
				schame = "443".equals(port) ? "https://" : "http://";
			}else{
				if(schame.contains(",")){
					schame = StringUtils.split(schame, ",")[0];
				}
				schame = schame + "://";
			}
			if(host.contains(",")){
				host = StringUtils.split(host, ",")[0];
			}
			baseUrl = schame + host + StringUtils.trimToEmpty(prefix);
		}
		
		if(withContextPath && StringUtils.isNotBlank(request.getContextPath())){
			baseUrl = baseUrl + request.getContextPath();
		}
		
		return baseUrl;
	}
	
	public static Map<String, String> getCustomHeaders(){
		Map<String, String> headers = new HashMap<>();
		 HttpServletRequest request = CurrentRuntimeContext.getRequest();
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
		String headerValue = request.getHeader(WebConstants.HEADER_INTERNAL_REQUEST);
		if(StringUtils.isNotBlank(headerValue)){
			try {
				TokenGenerator.validate(headerValue, true);
			} catch (Exception e) {
				headerValue = null;
			}
		}
		
		if(headerValue != null && Boolean.parseBoolean(headerValue)){
			return true;
		}

		//从网关转发
		headerValue = request.getHeader(WebConstants.HEADER_FORWARDED_GATEWAY);
		if(StringUtils.isNotBlank(headerValue)){
			try {
				TokenGenerator.validate(headerValue, true);
			} catch (Exception e) {
				headerValue = null;
			}
		}
		if(StringUtils.isNotBlank(headerValue)){
			return false;
		}
		
		boolean isInner = IpUtils.isInnerIp(request.getServerName());
		if(!isInner && !internalDomains.isEmpty()){
			isInner = internalDomains.stream().anyMatch(domain -> request.getServerName().endsWith(domain));
		}
		
		return isInner;
	}
	
	public static void printHeaders(HttpServletRequest request) {
		Enumeration<String> headerNames = request.getHeaderNames();
		String headerName;
		System.out.println("============Request Headers==============");
		while(headerNames.hasMoreElements()) {
			headerName = headerNames.nextElement();
			System.out.println(String.format("%s = %s", headerName,request.getHeader(headerName)));
		}
	}
}
