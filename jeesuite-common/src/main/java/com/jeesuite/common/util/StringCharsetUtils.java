package com.jeesuite.common.util;

import java.nio.charset.Charset;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2019年08月29日
 */
public class StringCharsetUtils {

	public static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");
    public static final Charset UTF_8 = Charset.forName("UTF-8");
    public static final Charset GBK = Charset.forName("GBK");
    public static final Charset UTF_16BE = Charset.forName("UTF-16BE");
    public static final Charset UTF_16LE = Charset.forName("UTF-16LE");
    
	public static boolean isCharset(String str, Charset charset) {
		try {
			return str.equals(new String(str.getBytes(charset), charset));
		} catch (Exception e) {
			return false;
		}
	}
	
	public static String convertCharset(String str, Charset toCharset)  {
		if(isCharset(str, toCharset))return str;
		if(isCharset(str, UTF_8)){
			return convertCharset(str, UTF_8, toCharset);
		}
		if(isCharset(str, GBK)){
			return convertCharset(str, UTF_8, toCharset);
		}
		if(isCharset(str, ISO_8859_1)){
			return convertCharset(str, UTF_8, toCharset);
		}
		return str;
	}

	public static String convertCharset(String str, Charset fromCharset, Charset toCharset)  {
		if (str != null) {
			byte[] bs = str.getBytes(fromCharset);
			return new String(bs, toCharset);
		}
		return null;
	}
}
