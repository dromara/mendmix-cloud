package com.jeesuite.security.util;

import java.nio.charset.StandardCharsets;

import com.jeesuite.common.crypt.Base64;
import com.jeesuite.common.crypt.DES;
import com.jeesuite.common.util.DigestUtils;
import com.jeesuite.common.util.ResourceUtils;

public class SecurityCryptUtils {

	private static String cryptKey;
	
	static{
		String base = ResourceUtils.getProperty("auth.crypt.key",SecurityCryptUtils.class.getName());
		cryptKey = base.substring(0, 2) + DigestUtils.md5Short(base);
	}
	
	public static String getCryptKey() {
		return cryptKey;
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

}
