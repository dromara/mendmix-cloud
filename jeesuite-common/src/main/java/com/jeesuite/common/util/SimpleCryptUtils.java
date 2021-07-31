package com.jeesuite.common.util;

import java.nio.charset.StandardCharsets;

import com.jeesuite.common.crypt.Base64;
import com.jeesuite.common.crypt.DES;

public class SimpleCryptUtils {

	public static final String GLOBAL_CRYPT_KEY;
	static{
		String env = ResourceUtils.getProperty("env");
		String base = ResourceUtils.getProperty("global.crypto.cryptKey",SimpleCryptUtils.class.getName());
		GLOBAL_CRYPT_KEY = DigestUtils.md5Short(env + base) + DigestUtils.md5(base).substring(0, 2);
	}
	public static String encrypt(String data) {
		 String encode = DES.encrypt(GLOBAL_CRYPT_KEY, data);
		 byte[] bytes = Base64.encodeToByte(encode.getBytes(StandardCharsets.UTF_8), false);
		 return new String(bytes, StandardCharsets.UTF_8);
	 }
	 
	 public static String decrypt(String data) {
		 byte[] bytes = Base64.decode(data);
		 data = new String(bytes, StandardCharsets.UTF_8);
		 return DES.decrypt(GLOBAL_CRYPT_KEY, data);
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
