package org.dromara.mendmix.common.crypt;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.common.util.DigestUtils;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月13日
 */
public class AesWrapper {
	
	private byte[] cryptKeyBytes;
	
	public AesWrapper() {
		this(UUID.randomUUID().toString());
	}
	
	public AesWrapper(String cryptKey) {
		StringBuilder sb = new StringBuilder();
		String keyMd5 = DigestUtils.md5(cryptKey);
		sb.append(keyMd5.substring(2,4)).append("#");
		sb.append(keyMd5.substring(5,10)).append("@");
		sb.append(keyMd5.substring(26,28).toUpperCase()).append("&");
		sb.append(keyMd5.substring(17,20)).append("$");
		this.cryptKeyBytes = sb.toString().getBytes();
	}

	public String encrypt(String plaintext) {
		if(StringUtils.isBlank(plaintext)){
			return null;
		}
		try {
			byte[] bytes = AES.encrypt(plaintext.getBytes(), cryptKeyBytes);
			return Base58.encode(bytes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public String decrypt(String ciphertext) {
		if(StringUtils.isBlank(ciphertext)){
			return null;
		}

		try {
			byte[] bytes = Base58.decode(ciphertext);
			bytes = AES.decrypt(bytes, cryptKeyBytes);
			return new String(bytes);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
}
