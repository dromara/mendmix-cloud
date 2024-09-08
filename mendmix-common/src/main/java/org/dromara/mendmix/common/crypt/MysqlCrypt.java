/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.common.crypt;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.common.util.DigestUtils;
import org.dromara.mendmix.common.util.ResourceUtils;

/**
 * 
 * mysql加解密函数
 * <br>
 * Class Name   : MysqlCrypt
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date Jun 15, 2021
 */
public class MysqlCrypt {
	   
		//select CONVERT(AES_DECRYPT(UNHEX(id_card),#{cryptKey}) USING utf8) as idcard from mdm_staff_basic where id=#{id}
		private static Pattern hexPattern = Pattern.compile("[0-9A-Fa-f]+");
		private static final String AES = "AES";
		private static SecretKeySpec secretKeySpec = null;
		
	    static{
	    	String secretKey = ResourceUtils.getProperty("sensitive.encrypt.secretkey","mendmix");
	    	secretKey = DigestUtils.md5Short(secretKey);
	    	secretKey = secretKey.substring(0,1) + secretKey + DigestUtils.md5(secretKey).substring(0,1);
	    	secretKeySpec = generateMySQLAESKey(secretKey, "UTF-8");
	    }

	    /**
	     * 加密
	     * @param text
	     * @return
	     */
	    public static String encrypt(String text) {
	    	if(StringUtils.isBlank(text))return null;
			try {
				final Cipher encryptCipher = Cipher.getInstance(AES);	        				
				encryptCipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);		
				String decode = new String(Hex.encodeHex(encryptCipher.doFinal(text.getBytes("UTF-8")))); 
				return decode.toUpperCase();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		
	    /**
	     * 解密
	     * @param encodeText
	     * @return
	     */
		public static String decrypt(String encodeText) {
			if(StringUtils.isBlank(encodeText))return null;
		    try {
		    	final Cipher decryptCipher = Cipher.getInstance(AES);	        				
				decryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
				String result = new String(decryptCipher.doFinal(Hex.decodeHex(encodeText.toCharArray())));
		        return result;
		    } catch (Exception e) {
		    	System.err.println("encodeText:" + encodeText);
		    	if(!hexPattern.matcher(encodeText).matches()){
		    		throw new RuntimeException("字符串不是16进制格式");
		    	}
		    	throw new RuntimeException(e);
		    }
		}

		
		private static SecretKeySpec generateMySQLAESKey(final String key, final String encoding) {
			try {
				final byte[] finalKey = new byte[16];
				int i = 0;
				for(byte b : key.getBytes(encoding))
					finalKey[i++%16] ^= b;			
				return new SecretKeySpec(finalKey, AES);
			} catch(UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
	 
		public static void main(String... args) throws Exception {
			System.out.println(decrypt("292F9D4CF048477797AC56FD233BB505E56F5E120D8A336CDCA32984CA4B0651"));
		}
}
