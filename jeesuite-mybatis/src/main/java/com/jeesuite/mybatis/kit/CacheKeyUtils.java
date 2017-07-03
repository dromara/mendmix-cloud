/**
 * 
 */
package com.jeesuite.mybatis.kit;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月1日
 */
public class CacheKeyUtils {
	
	private static final String CARTSET_UTF_8 = "UTF-8";
	private static final String MD5_NAME = "MD5";
	
	/**
	 * MD5加密
	 * @param content
	 * @return
	 */
	private static String md5(Object content) {
		String keys = null;
		if (content == null) {
			return null;
		}
		try {
			MessageDigest md = MessageDigest.getInstance(MD5_NAME);
			byte[] bPass = String.valueOf(content).getBytes(CARTSET_UTF_8);
			md.update(bPass);
			keys = bytesToHexString(md.digest());
		} catch (NoSuchAlgorithmException aex) {
			System.out.println(aex);
		} catch (java.io.UnsupportedEncodingException uex) {
			System.out.println(uex);
		}
		return keys.toLowerCase();
	}
	
	private static String bytesToHexString(byte[] bArray) {
		StringBuffer sb = new StringBuffer(bArray.length);
		String sTemp;
		for (int i = 0; i < bArray.length; i++) {
			sTemp = Integer.toHexString(0xFF & bArray[i]);
			if (sTemp.length() < 2) {
				sb.append(0);
			}
			sb.append(sTemp.toUpperCase());
		}
		return sb.toString();
	}
	
	public static String toString(Object obj){
		if(obj == null)return "_";
		if(isSimpleDataType(obj)){
			return obj.toString();
		}else if(obj instanceof Collection){
			String toString = obj.toString().replaceAll("\\s{0,}", "");
			return toString.length() > 32 ? md5(toString) : toString;
		}else{
			String toString = ToStringBuilder.reflectionToString(obj, ToStringStyle.SHORT_PREFIX_STYLE);
			
			return toString.length() > 32 ? md5(toString) : toString;
		}
	}
	
	/**
	 * 生成缓存key
	 * @param prefix
	 * @param args
	 * @return
	 */
	public static String generate(String prefix,Object...args){
		if(args == null || args.length == 0)return prefix;
		StringBuilder keyBuilder = new StringBuilder(prefix);
		
		String keyString = null;
		if(args != null && args.length > 0){
			keyBuilder.append(":");
			StringBuilder argsBuilder = new StringBuilder();
			for (int i = 0; i < args.length; i++) {
				if(args[i] == null){
					argsBuilder.append("_null");
				}else if(isSimpleDataType(args[i])){
					argsBuilder.append(args[i].toString());
				}else if(args[i] instanceof Collection){
					argsBuilder.append(args[i].toString().replaceAll("\\s{0,}", ""));
				}else{
					argsBuilder.append(ToStringBuilder.reflectionToString(args[i], ToStringStyle.SHORT_PREFIX_STYLE));
				}
				if(i < args.length - 1)argsBuilder.append("_");
			}
			keyString = argsBuilder.length() > 32 ? md5(argsBuilder) : argsBuilder.toString();
			keyBuilder.append(keyString);
		}
		return keyBuilder.toString();
	}
	
	 private static boolean isSimpleDataType(Object o) {   
		   Class<? extends Object> clazz = o.getClass();
	       return 
	       (   
	           clazz.equals(String.class) ||   
	           clazz.equals(Integer.class)||   
	           clazz.equals(Byte.class) ||   
	           clazz.equals(Long.class) ||   
	           clazz.equals(Double.class) ||   
	           clazz.equals(Float.class) ||   
	           clazz.equals(Character.class) ||   
	           clazz.equals(Short.class) ||   
	           clazz.equals(BigDecimal.class) ||     
	           clazz.equals(Boolean.class) ||   
	           clazz.equals(Date.class) ||   
	           clazz.isPrimitive()   
	       );   
	}
	 
	public static void main(String[] args) {
		List<Object> ids = new ArrayList<>();
		ids.add(1);
		ids.add("2ff ");
		ids.add(2);
		
		System.out.println(generate("demo_", ids));
		System.out.println(generate("demo_", ids));
		
		String[] arr = new String[]{"1","b"};
		System.out.println(generate("demo_", arr));
	} 
}
