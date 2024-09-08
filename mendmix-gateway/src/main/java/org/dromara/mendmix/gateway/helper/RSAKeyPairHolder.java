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
package org.dromara.mendmix.gateway.helper;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.cache.CacheExpires;
import org.dromara.mendmix.cache.CacheUtils;
import org.dromara.mendmix.common.GlobalContext;
import org.dromara.mendmix.common.crypt.RSA;
import org.dromara.mendmix.common.crypt.RSA.RSAKeyPair;
import org.dromara.mendmix.common.util.ResourceUtils;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年4月27日
 */
public class RSAKeyPairHolder {

	private static String publicKeyString;
	private static RSAPrivateKey appPrivateKey; // 应用自身私钥
	private static RSAPublicKey appPublicKey;// 应用自身公钥

	static {
		String privateyKeyString = ResourceUtils.getProperty("mendmix-cloud.decrypt.privateyKey");
		publicKeyString = ResourceUtils.getProperty("mendmix-cloud.decrypt.publicKey");
		if(privateyKeyString == null) {
			privateyKeyString = CacheUtils.getStr("privateyKey:" + GlobalContext.SYSTEM_KEY);
			publicKeyString = CacheUtils.getStr("publicKey:" + GlobalContext.SYSTEM_KEY);
		}
		
		if (StringUtils.isNoneBlank(privateyKeyString, publicKeyString)) {
			try {
				appPrivateKey = RSA.getPrivateKey(privateyKeyString);
				appPublicKey = RSA.getPublicKey(publicKeyString);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		//
		if (appPublicKey == null) {
			RSAKeyPair rsaKeyPair = RSA.generateKeyPair(1024);
			appPrivateKey = rsaKeyPair.getPrivateKey();
			appPublicKey = rsaKeyPair.getPublicKey();
			publicKeyString = Base64.getEncoder().encodeToString(appPublicKey.getEncoded());
			privateyKeyString = Base64.getEncoder().encodeToString(appPrivateKey.getEncoded());
			CacheUtils.setStr("privateyKey:" + GlobalContext.SYSTEM_KEY, privateyKeyString,CacheExpires.todayEndSeconds());
			CacheUtils.setStr("publicKey:" + GlobalContext.SYSTEM_KEY, publicKeyString,CacheExpires.todayEndSeconds());
		}
	}

	public static RSAPrivateKey getPrivateKey() {
		return appPrivateKey;
	}

	public static RSAPublicKey getPublicKey() {
		return appPublicKey;
	}

	public static String getPublicKeyString() {
		return publicKeyString;
	}
	
	

}
