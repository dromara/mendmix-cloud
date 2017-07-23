package com.jeesuite.common.util;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.jeesuite.common.crypt.Base64;
import com.jeesuite.common.crypt.DES;
import com.jeesuite.common.util.DigestUtils;

public class SimpleCryptUtils {

	private static final String KEY_TAIL = "j@";
	
	public static String encrypt(String key,String data) {
		 key = DigestUtils.md5Short(key) + KEY_TAIL;
		 String encode = DES.encrypt(key, data);
		 byte[] bytes = Base64.encodeToByte(encode.getBytes(StandardCharsets.UTF_8), true);
		 return new String(bytes, StandardCharsets.UTF_8);
	 }
	 
	 public static String decrypt(String key,String data) {
		 key = DigestUtils.md5Short(key) + KEY_TAIL;
		 byte[] bytes = Base64.decode(data);
		 data = new String(bytes, StandardCharsets.UTF_8);
		 return DES.decrypt(key, data);
	 }
	 
	 
	 public static void main(String[] args) {
		 long s = System.currentTimeMillis();
		 for (int i = 0; i < 2; i++) {			
			 String key = UUID.randomUUID().toString().replaceAll("-", "");
			 String data = UUID.randomUUID().toString().replaceAll("-", "").substring(0,16);
			 String encode = encrypt(key, data);
			 if(!data.equals(decrypt(key, encode))){
				 System.out.println(encode);
			 }
 
		}
		 
		 System.out.println(System.currentTimeMillis() - s );
	}
}
