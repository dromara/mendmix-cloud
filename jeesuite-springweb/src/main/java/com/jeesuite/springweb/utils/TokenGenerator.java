package com.jeesuite.springweb.utils;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.util.DigestUtils;

public class TokenGenerator {

	private static final String LINE_THROUGH = "-";
	private static final int EXPIRE = 1000*60*3;

	public static String generate(String...prefixs){
		String str = StringUtils.replace(UUID.randomUUID().toString(), LINE_THROUGH, StringUtils.EMPTY);
		if(prefixs != null && prefixs.length > 0 &&  StringUtils.isNotBlank(prefixs[0])){
			return prefixs[0].concat(str);
		}
		return str;
	}
	
	public static String generateWithSign(){
		String str = DigestUtils.md5Short(generate()).concat(String.valueOf(System.currentTimeMillis()));	
		return SecurityCryptUtils.encrypt(str);
	}
	
	public static void validate(String token,boolean validateExpire){
		long timestamp = 0;
		try {
			timestamp = Long.parseLong(SecurityCryptUtils.decrypt(token).substring(6));
		} catch (Exception e) {
			throw new JeesuiteBaseException(4005, "sessionId格式不正确");
		}
		if(validateExpire && System.currentTimeMillis() - timestamp > EXPIRE){
			throw new JeesuiteBaseException(4005, "sessionId过期");
		}
	}
	
	public static void main(String[] args) {
		System.out.println(TokenGenerator.generate());
	}
}
