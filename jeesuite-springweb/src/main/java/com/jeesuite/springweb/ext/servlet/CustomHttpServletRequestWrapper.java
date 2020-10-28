package com.jeesuite.springweb.ext.servlet;

import java.io.IOException;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.apache.commons.lang3.StringEscapeUtils;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年3月7日
 */
public class CustomHttpServletRequestWrapper extends HttpServletRequestWrapper {

	private byte[] streams;
    public CustomHttpServletRequestWrapper(HttpServletRequest request,byte[] streams) {  
        super(request);  
        this.streams = streams;
    }  
    
    
    @Override
	public ServletInputStream getInputStream() throws IOException {
		return new CustomServletInputStreamWrapper(streams); 
	}

	@Override
	public int getContentLength() {
		return streams.length; 
	}

	@Override
	public long getContentLengthLong() {
		return streams.length; 
	}
      
      
     @Override    
     public String getParameter(String name) {    
          String value = super.getParameter(name);    
            if (value != null) {    
                value = StringEscapeUtils.escapeEcmaScript(value);    
            }    
            return value;    
     }    
       
     @Override  
    public String[] getParameterValues(String name) {  
         String[] values = super.getParameterValues(name);  
         if(values != null && values.length > 0){  
             for(int i =0; i< values.length ;i++){  
                 values[i] = StringEscapeUtils.escapeEcmaScript(values[i]);  
             }  
         }  
        return values;  
     }  
       
     @Override    
     public String getHeader(String name) {    
        
            String value = super.getHeader(name);    
            if (value != null) {    
                value = StringEscapeUtils.escapeEcmaScript(value);    
            }    
            return value;    
        }  
}
