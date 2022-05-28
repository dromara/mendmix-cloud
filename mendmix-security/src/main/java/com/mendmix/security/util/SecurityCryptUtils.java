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
package com.mendmix.security.util;

import java.nio.charset.StandardCharsets;

import com.mendmix.common.crypt.Base64;
import com.mendmix.common.crypt.DES;
import com.mendmix.common.util.DigestUtils;
import com.mendmix.common.util.ResourceUtils;

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
