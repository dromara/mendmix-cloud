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

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.mendmix.common.crypt.Base64;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2012年6月17日
 */
public class DigestUtils {

	private static final String CARTSET_UTF_8 = "UTF-8";
	private static final String MD5_NAME = "MD5";
	private static final char[] saltChars = ("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789./"
			.toCharArray());
	
	
	/**
	 * MD5加密
	 * @param content
	 * @return
	 */
	public static String md5(Object content) {
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
	
	public static String md5WithSalt(Object content,String salt) {
		if (content == null) {
			return null;
		}
		
		return md5(content.toString().concat(salt));
	}
	

	/**
	 * 生成MD5短码（6位）
	 * @param orig
	 * @return
	 */
	public static String md5Short(String orig){
		if(orig == null){
			return null ;
		}
		//先得到url的32个字符的md5码
		String md5 = DigestUtils.md5(orig) ;
		//将32个字符的md5码分成4段处理，每段8个字符
		//4段任意一段加密都可以，这里我们取第二段
		int offset = 1 * 8 ;
		String sub = md5.substring(offset, offset + 8) ; 
		long sub16 = Long.parseLong(sub , 16) ; //将sub当作一个16进制的数，转成long  
		// & 0X3FFFFFFF，去掉最前面的2位，只留下30位
		sub16 &= 0X3FFFFFFF ;

		StringBuilder sb = new StringBuilder() ;
		//将剩下的30位分6段处理，每段5位
		for (int j = 0; j < 6 ; j++) {
			//得到一个 <= 61的数字
			long t = sub16 & 0x0000003D ;
			sb.append(saltChars[(int) t]) ;
			sub16 >>= 5 ;  //将sub16右移5位

		}
		return sb.toString() ;
	}
	
	public static String encodeBase64(String string){
		try {
			return Base64.encodeToString(string.getBytes(CARTSET_UTF_8), false);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
	
	
	public static String decodeBase64(String string){
		try {
			return new String(Base64.decodeFast(string.getBytes(CARTSET_UTF_8)));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
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
	
	public static void main(String[] args) {
		System.out.println(md5Short("3rg56yyyyyyyy"));
	}

}
