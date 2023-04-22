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
package com.mendmix.gateway.helper;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.cache.CacheExpires;
import com.mendmix.cache.CacheUtils;
import com.mendmix.common.GlobalRuntimeContext;
import com.mendmix.common.crypt.RSA;
import com.mendmix.common.crypt.RSA.RSAKeyPair;
import com.mendmix.common.util.ResourceUtils;

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
		String privateyKeyString = ResourceUtils.getProperty("application.decrypt.privateyKey");
		publicKeyString = ResourceUtils.getProperty("application.decrypt.publicKey");
		if(privateyKeyString == null) {
			privateyKeyString = CacheUtils.getStr("privateyKey:" + GlobalRuntimeContext.SYSTEM_KEY);
			publicKeyString = CacheUtils.getStr("publicKey:" + GlobalRuntimeContext.SYSTEM_KEY);
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
			CacheUtils.setStr("privateyKey:" + GlobalRuntimeContext.SYSTEM_KEY, privateyKeyString,CacheExpires.todayEndSeconds());
			CacheUtils.setStr("publicKey:" + GlobalRuntimeContext.SYSTEM_KEY, publicKeyString,CacheExpires.todayEndSeconds());
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
