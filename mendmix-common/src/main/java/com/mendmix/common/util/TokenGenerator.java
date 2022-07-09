/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.common.util;

import java.util.Date;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.MendmixBaseException;
import com.mendmix.common.crypt.Base58;

/**
 * 生成token
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年11月30日
 */
public class TokenGenerator {

	private static final String LINE_THROUGH = "-";
	private static final int EXPIRE = 1000*60*3;
	
	public static String generateFrom(String base){
		String str = DigestUtils.md5(base);
		return new String(Base58.encode(str.getBytes())); 
	}

	public static String generate(String...prefixs){
		String str = StringUtils.replace(UUID.randomUUID().toString(), LINE_THROUGH, StringUtils.EMPTY);
		if(prefixs != null && prefixs.length > 0 &&  StringUtils.isNotBlank(prefixs[0])){
			str = prefixs[0].concat(str);
		}
		return str;
	}
	
	public static String generateWithSign(){
		return generateWithSign(null);
	}
	/**
	 * 生成带签名信息的token
	 * @return
	 */
	public static String generateWithSign(String tokenType){
		String timeString = String.valueOf(System.currentTimeMillis());
		String str = DigestUtils.md5Short(timeString).concat(timeString);	
		return SimpleCryptUtils.encrypt(str);
	}
	
	
	public static void validate(String token,boolean validateExpire){
		validate(null, token, validateExpire);
	}
	
	/**
	 * 验证带签名信息的token
	 */
	public static void validate(String tokenType,String token,boolean validateExpire){
		long timestamp;
		Date date = new Date();
		try {
			timestamp = Long.parseLong(SimpleCryptUtils.decrypt(token).substring(6));
		} catch (Exception e) {
			throw new MendmixBaseException(403, "token格式错误");
		}
		if(validateExpire && date.getTime() - timestamp > EXPIRE){
			throw new MendmixBaseException(403, "token已过期");
		}
	}
	
	public static void main(String[] args) {
		String token  = generateWithSign();
		System.out.println(token);
		validate(token, true);
	}
}
