package com.jeesuite.common.util;

import java.nio.charset.StandardCharsets;

import com.jeesuite.common.crypt.Base64;
import com.jeesuite.common.crypt.DES;

public class SimpleCryptUtils {

	private static String cryptKey = ResourceUtils.getProperty("global.crypt.secretKey",SimpleCryptUtils.class.getName());
	static{
		cryptKey = DigestUtils.md5Short(cryptKey) + DigestUtils.md5(cryptKey).substring(0, 2);
	}
	public static String encrypt(String data) {
		 String encode = DES.encrypt(cryptKey, data);
		 byte[] bytes = Base64.encodeToByte(encode.getBytes(StandardCharsets.UTF_8), false);
		 return new String(bytes, StandardCharsets.UTF_8);
	 }
	 
	 public static String decrypt(String data) {
		 byte[] bytes = Base64.decode(data);
		 data = new String(bytes, StandardCharsets.UTF_8);
		 return DES.decrypt(cryptKey, data);
	 }
	 
	 
	 public static String encrypt(String cryptKey,String data) {
		 String encode = DES.encrypt(cryptKey, data);
		 byte[] bytes = Base64.encodeToByte(encode.getBytes(StandardCharsets.UTF_8), false);
		 return new String(bytes, StandardCharsets.UTF_8);
	 }
	 
	 public static String decrypt(String cryptKey,String data) {
		 byte[] bytes = Base64.decode(data);
		 data = new String(bytes, StandardCharsets.UTF_8);
		 return DES.decrypt(cryptKey, data);
	 }
	
}
