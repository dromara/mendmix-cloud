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
package org.dromara.mendmix.common.util;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.dromara.mendmix.common.MendmixBaseException;
import org.dromara.mendmix.common.crypt.Base64;

/**
 * RSA签名工具
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @website <a href="http://dromara.org">vakin</a>
 * @date 2019年5月10日
 */
public class RsaSignUtils {

	private static final String KEY_ALGORITHM = "RSA";

	private static final String SIGN_ALGORITHM = "SHA256withRSA";
	
    /** 貌似默认是RSA/NONE/PKCS1Padding，未验证 */
    private static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";

    /** RSA密钥长度必须是64的倍数，在512~65536之间。默认是1024 */
    private static final int KEY_SIZE = 1024; 
    
    /** 
     * RSA最大加密明文大小:明文长度(bytes) <= 密钥长度(bytes)-11
     */  
    private static final int MAX_ENCRYPT_BLOCK = KEY_SIZE / 8 - 11 ;  
      
    /** 
     * RSA最大解密密文大小 
     */  
    private static final int MAX_DECRYPT_BLOCK = KEY_SIZE / 8; 

	public static PublicKey loadPublicKey(String pubKeyString) {
		byte[] keyBytes = Base64.decode(pubKeyString);
		X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);
		try {
			KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
			PublicKey publicKey = factory.generatePublic(x509EncodedKeySpec);
			return publicKey;
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {}
		return null;
	}

	public static PrivateKey loadPrivateKey(String privateKeyString) {
		try {
			byte[] privateKeyBytes = Base64.decode(privateKeyString);
			PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			return keyFactory.generatePrivate(pkcs8KeySpec);
		} catch (Exception e) {
			// TODO: handle exception
		}
		return null;
	}

	public static String encrypt(PublicKey key, String plainText) {
		byte[] encodeBytes = encrypt(key, plainText.getBytes(StandardCharsets.UTF_8));
		return Base64.encodeToString(encodeBytes, false);
	}

	public static byte[] encrypt(PublicKey key, byte[] plainBytes) {

		ByteArrayOutputStream out = null;
		try {
			Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, key);

			int inputLen = plainBytes.length;
			if (inputLen <= MAX_ENCRYPT_BLOCK) {
				return cipher.doFinal(plainBytes);
			}
			out = new ByteArrayOutputStream();
			int offSet = 0;
			byte[] cache;
			int i = 0;
			// 对数据分段加密
			while (inputLen - offSet > 0) {
				if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
					cache = cipher.doFinal(plainBytes, offSet, MAX_ENCRYPT_BLOCK);
				} else {
					cache = cipher.doFinal(plainBytes, offSet, inputLen - offSet);
				}
				out.write(cache, 0, cache.length);
				i++;
				offSet = i * MAX_ENCRYPT_BLOCK;
			}
			return out.toByteArray();
		} catch (NoSuchAlgorithmException e) {
			throw new MendmixBaseException(4003,"无此解密算法");
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			throw new MendmixBaseException(4003,"解密私钥非法,请检查");
		} catch (IllegalBlockSizeException e) {
			throw new MendmixBaseException(4003,"密文长度非法");
		} catch (BadPaddingException e) {
			throw new MendmixBaseException(4003,"密文数据已损坏");
		} finally {
			try {
				if (out != null)
					out.close();
			} catch (Exception e2) {
			}
		}
	}
	
	public static String decrypt(PrivateKey key, String encodedText) {
    	byte[] bytes = Base64.decode(encodedText);
    	return decrypt(key, bytes);
    }
	
	
	 public static String decrypt(PrivateKey key, byte[] encodedText) {

	    	ByteArrayOutputStream out = null;
	        try {
	            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
	            cipher.init(Cipher.DECRYPT_MODE, key);
	            int inputLen = encodedText.length; 
	            
	            if(inputLen <= MAX_DECRYPT_BLOCK){
	            	return new String(cipher.doFinal(encodedText),StandardCharsets.UTF_8);
	            }
	            
	            out = new ByteArrayOutputStream();  
	            int offSet = 0;  
	            byte[] cache;  
	            int i = 0;  
	            // 对数据分段解密  
	            while (inputLen - offSet > 0) {  
	                if (inputLen - offSet > MAX_DECRYPT_BLOCK) {  
	                    cache = cipher.doFinal(encodedText, offSet, MAX_DECRYPT_BLOCK);  
	                } else {  
	                    cache = cipher.doFinal(encodedText, offSet, inputLen - offSet);  
	                }  
	                out.write(cache, 0, cache.length);  
	                i++;  
	                offSet = i * MAX_DECRYPT_BLOCK;  
	            }  
	            return new String(out.toByteArray(),StandardCharsets.UTF_8);
	        } catch (NoSuchAlgorithmException e) {  
	            throw new MendmixBaseException(4003,"无此解密算法");  
	        } catch (NoSuchPaddingException e) {  
	            e.printStackTrace();  
	            return null;  
	        } catch (InvalidKeyException e) {  
	            throw new MendmixBaseException(4003,"解密私钥非法,请检查");  
	        } catch (IllegalBlockSizeException e) {  
	            throw new MendmixBaseException(4003,"密文长度非法");  
	        } catch (BadPaddingException e) {  
	            throw new MendmixBaseException(4003,"密文数据已损坏");  
	        }finally{
	        	try {if(out != null)out.close(); } catch (Exception e2) {}
	        }
	    }
	    

	public static String signature(PrivateKey privateKey,String contents) {
		try {
			byte[] data = contents.getBytes(StandardCharsets.UTF_8.name());
			Signature signature = Signature.getInstance(SIGN_ALGORITHM);
			signature.initSign(privateKey);
			signature.update(data);

			return Base64.encodeToString(signature.sign(), false);
		} catch (NoSuchAlgorithmException e) {
			// TODO: handle exception
		} catch (InvalidKeyException e) {
			throw new MendmixBaseException(4003,"私钥格式错误");
		} catch (SignatureException e) {
			// TODO: handle exception
		} catch (UnsupportedEncodingException e) {
			// TODO: handle exception
		}

		return null;
	}

	public static boolean verifySignature(String contents, String sign, PublicKey publicKey) {
		try {
			byte[] data = contents.getBytes(StandardCharsets.UTF_8.name());
			byte[] dataSignature = Base64.decode(sign);

			Signature signature = Signature.getInstance(SIGN_ALGORITHM);
			signature.initVerify(publicKey);
			signature.update(data, 0, data.length);
			return signature.verify(dataSignature);
		} catch (Exception e) {
			return false;
		}

	}
	
    /** 
     * 随机生成密钥对 
     */  
    public static String[] generateKeyPair() {  
        // KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象  
        KeyPairGenerator keyPairGen = null;  
        try {  
            keyPairGen = KeyPairGenerator.getInstance(KEY_ALGORITHM);  
        } catch (NoSuchAlgorithmException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }  
        // 初始化密钥对生成器，密钥大小为96-1024位  
        keyPairGen.initialize(KEY_SIZE,new SecureRandom());  
        // 生成一个密钥对，保存在keyPair中  
        KeyPair keyPair = keyPairGen.generateKeyPair();  
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();  
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();  
        String publicKeyString = Base64.encodeToString(publicKey.getEncoded(),true);  
        String privateKeyString = Base64.encodeToString(privateKey.getEncoded(),true); 
        
        return new String[]{publicKeyString,privateKeyString};
    } 

	public static void main(String[] args) {
		
		
		PublicKey publicKey = loadPublicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDa2AxOvOQwvL1WHzjPPuZqFUkZLxG3dluThj3/leLygtJt3aNeAq8mKNEdAciD8uYfmQ4MrlAhvwvCCcEN9Ka6hkn6pUOzNZNC5P265vNqpU7GwL3yACsXHdRod9yVN3jVMZp5BR6+tLWLG2BEfmgvQD+8NMSwj9Aq2YvH3AdVOwIDAQAB");
		
		String contents = "appId=60060&data={amount=1000&attach=abc1234&customCompanyId=55108&invoiceSubjectId=10005&invoiceType=PP&notifyUrl=http://192.168.1.94:11841/out/invoiceNotifySuc&outOrderNo=1234&remark=1231234&serviceCompanyId=1000}&method=ayg.invoice.invoiceApply&nonce=a9607bd2b9ff4872a99ea02c67dda8a8&timestamp=2018-07-17 10:47:54&version=1.0";
		String sign = "VY+PJ4IyOnVxVAZ+AODBfH9kXlHY+PJDKxJNwtEIp9XQJio9lMsDdxAPABm59zWuY01vZdc5bRVjwGT1FuFIMzD8mUg3MVfRFNS7zoefQOw/JvNHvCXaTv9YcbKzW+x3/tl3fUn6Z/ytGE7NC20O9f4GNgJhBipsslFDyrBFeso=";
		System.out.println("sign --> " + verifySignature(contents, sign, publicKey));

	}
}
