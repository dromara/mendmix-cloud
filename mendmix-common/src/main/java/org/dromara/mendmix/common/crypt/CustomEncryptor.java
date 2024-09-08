package org.dromara.mendmix.common.crypt;

import org.apache.commons.lang3.StringUtils;

import org.dromara.mendmix.common.GlobalConstants;
import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.util.DigestUtils;
import org.dromara.mendmix.common.util.ResourceUtils;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月13日
 */
public class CustomEncryptor {

	private static CustomEncryptor defaultEncryptor;
	
	private long timeOffsetMillis;
	private boolean timeliness;
	private byte[] cryptKeyBytes;

	public CustomEncryptor(String cryptKey,boolean timeliness) {
		this(cryptKey, timeliness, 60 * 1000 * 5);
	}
	
	public static CustomEncryptor useDefault() {
		if(defaultEncryptor == null) {
			synchronized (CustomEncryptor.class) {	
				String secretKey = ResourceUtils.getAnyProperty("mendmix-cloud.crypto.secret","global.crypto.secret");
				if(secretKey == null)secretKey = GlobalConstants.DEFAULT_CRIPT_KEY;
				defaultEncryptor = new CustomEncryptor(secretKey, false);
			}
		}
		return defaultEncryptor;
	}

	public CustomEncryptor(String cryptKey,boolean timeliness,long timeOffsetMillis) {
		this.timeliness = timeliness;
		this.timeOffsetMillis = timeOffsetMillis;
		StringBuilder sb = new StringBuilder();
		String keyMd5 = DigestUtils.md5(cryptKey);
		sb.append(keyMd5.substring(2, 4)).append("#");
		sb.append(keyMd5.substring(5, 10)).append("@");
		sb.append(keyMd5.substring(26, 28).toUpperCase()).append("&");
		sb.append(keyMd5.substring(17, 20)).append("$");
		this.cryptKeyBytes = sb.toString().getBytes();
	}
	
	public String encrypt(String plaintext) {
		return encrypt(plaintext, true);
	}

	public String encrypt(String plaintext,boolean withPrefix) {
		if (StringUtils.isBlank(plaintext)) {
			return null;
		}
		try {
			if(timeliness) {
				plaintext = plaintext.concat(String.valueOf(System.currentTimeMillis()));
			}
			byte[] bytes = AES.encrypt(plaintext.getBytes(), cryptKeyBytes);
			String ciphertext = Base58.encode(bytes);
			return withPrefix ? GlobalConstants.CRYPT_PREFIX + ciphertext : ciphertext;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String decrypt(String ciphertext) {
		if (StringUtils.isBlank(ciphertext)) {
			return null;
		}
		if(ciphertext.startsWith(GlobalConstants.CRYPT_PREFIX)) {
			ciphertext = ciphertext.replace(GlobalConstants.CRYPT_PREFIX, StringUtils.EMPTY);
		}
		try {
			byte[] bytes = Base58.decode(ciphertext);
			bytes = AES.decrypt(bytes, cryptKeyBytes);
			String plaintext = new String(bytes);
			if(timeliness) {
				long timestamp = Long.parseLong(plaintext.substring(plaintext.length() - 13));
				if(System.currentTimeMillis() - timestamp > timeOffsetMillis) {
					throw new MendmixBaseException("token已失效");
				}
				plaintext = plaintext.substring(0,plaintext.length() - 13);
			}
			return plaintext;
		}catch (MendmixBaseException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void main(String[] args) {
		CustomEncryptor aesWrapper = new CustomEncryptor("xyz", true);
		String encrypt = aesWrapper.encrypt("hello");
		System.out.println(encrypt);
		System.out.println(aesWrapper.decrypt(encrypt));
	}
}