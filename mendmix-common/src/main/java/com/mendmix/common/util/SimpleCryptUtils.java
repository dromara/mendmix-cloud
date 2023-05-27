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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import com.mendmix.common.crypt.AesWrapper;

public class SimpleCryptUtils {

	public static final AesWrapper aesWrapper;
	static {
		String cryptKey = ResourceUtils.getProperty("mendmix.crypto.cryptKey");
		Validate.notBlank(cryptKey, "config[mendmix.crypto.cryptKey] is missing");
		aesWrapper = new AesWrapper(cryptKey);
	}

	public static String encrypt(String plaintext) {
		return aesWrapper.encrypt(plaintext);
	}

	public static String decrypt(String ciphertext) {
		return aesWrapper.decrypt(ciphertext);
	}

	public static void main(String[] args) {
		String ciphertext = encrypt("hello");
		System.out.println("ciphertext:" + ciphertext);
		String plaintext = decrypt(ciphertext);
		System.out.println("plaintext:" + plaintext);
	}

}
