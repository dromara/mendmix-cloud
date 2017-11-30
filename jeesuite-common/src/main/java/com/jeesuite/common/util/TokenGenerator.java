package com.jeesuite.common.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.JeesuiteBaseException;
import com.jeesuite.common.crypt.DES;

/**
 * 生成token
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年11月30日
 */
public class TokenGenerator {

	private static final String LINE_THROUGH = "-";
	private static final int EXPIRE = 1000*60*3;
	private static final String DES_CRYPT_KEY = ResourceUtils.getProperty("des.crypt.key");

	public static String generate(String...prefixs){
		String str = StringUtils.replace(UUID.randomUUID().toString(), LINE_THROUGH, StringUtils.EMPTY);
		if(prefixs != null && prefixs.length > 0 &&  StringUtils.isNotBlank(prefixs[0])){
			return prefixs[0].concat(str);
		}
		return str;
	}
	
	/**
	 * 生成带签名信息的token
	 * @return
	 */
	public static String generateWithSign(){
		Date date = new Date();
		String cryptKey = getCryptKey(date);
		String str = DigestUtils.md5Short(generate()).concat(String.valueOf(date.getTime()));	
		return DES.encrypt(cryptKey, str).toLowerCase();
	}
	
	
	/**
	 * 验证带签名信息的token
	 */
	public static void validate(String token,boolean validateExpire){
		long timestamp = 0;
		Date date = new Date();
		String cryptKey = getCryptKey(date);
		try {
			timestamp = Long.parseLong(DES.decrypt(cryptKey,token).substring(6));
		} catch (Exception e) {
			throw new JeesuiteBaseException(4005, "格式不正确");
		}
		if(validateExpire && date.getTime() - timestamp > EXPIRE){
			throw new JeesuiteBaseException(4005, "token已过期");
		}
	}
	
	private static String getCryptKey(Date date){
		if(DES_CRYPT_KEY != null && DES_CRYPT_KEY.length() == 8)return DES_CRYPT_KEY;
		SimpleDateFormat format = new SimpleDateFormat("ddMMMyy", Locale.ENGLISH);
        String key = format.format(date).toUpperCase();
        key  = DigestUtils.md5(key).substring(0,8);
		return key;
	}
	
	public static void main(String[] args) {
		String generateWithSign = TokenGenerator.generateWithSign();
		System.out.println(generateWithSign);
		
		TokenGenerator.validate(generateWithSign, true);
		
	}
}
