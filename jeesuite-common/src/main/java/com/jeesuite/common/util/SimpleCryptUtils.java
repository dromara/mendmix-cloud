package com.jeesuite.common.util;

import java.nio.charset.StandardCharsets;

import com.jeesuite.common.crypt.Base64;
import com.jeesuite.common.crypt.DES;

public class SimpleCryptUtils {

	private static final String KEY_TAIL = "j@";
	
	public static String encrypt(String key,String data) {
		 key = DigestUtils.md5Short(key) + KEY_TAIL;
		 String encode = DES.encrypt(key, data);
		 byte[] bytes = Base64.encodeToByte(encode.getBytes(StandardCharsets.UTF_8), false);
		 return new String(bytes, StandardCharsets.UTF_8);
	 }
	 
	 public static String decrypt(String key,String data) {
		 key = DigestUtils.md5Short(key) + KEY_TAIL;
		 byte[] bytes = Base64.decode(data);
		 data = new String(bytes, StandardCharsets.UTF_8);
		 return DES.decrypt(key, data);
	 }
	
}
