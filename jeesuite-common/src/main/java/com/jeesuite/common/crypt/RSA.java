package com.jeesuite.common.crypt;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class RSA {
	private static final String DEFAULT_ENCODING = "UTF-8";

	private static final Charset DEFAULT_CHARSET = Charset.forName(DEFAULT_ENCODING);
	
	private static final String KEY_ALGORITHM = "RSA";
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
  

    public static void main(String[] args) throws Exception {
    	
    	String PLAIN_TEXT = "srtt46y7u";
    	 PrivateKey privateKey = loadPrivateKeyFromKeyStore("/Users/jiangwei/payment.jks", "payment", "JCEKS", "m5cidi9p3eds0", "v6ol0d31y8hd9c");
         // 加密
         PublicKey publicKey = loadPublicKeyFromKeyStore("/Users/jiangwei/configcenter.jks", "payment", "JCEKS", "a3m5v6o8yc9d", "a3m5v6o8yc9d");
         
         String encodedText = encrypt(publicKey, PLAIN_TEXT);
         System.out.println("RSA encoded: " + encodedText);

         // 解密
         System.out.println("RSA decoded: "  + decrypt(privateKey, "Kli0lCJbkdDAPGBYCa/755kGBreS9F9FsFiWiT3eUNq+aZLoK5nL2qy/MOpjjn4NwJdC07zJ54FmhWfNkoO1/FQsGhAjWoYfQFlox1fvoAiyTjiFiYt9F40P4jTHPZplYUuzEx5WIRpBvdNuQ+YYqKjJRu01TjpYV1kW5Hu5/rI="));
    }

    /** 
     * 随机生成密钥对 
     */  
    public static void generateKeyPair(String filePath) {  
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
        // 得到私钥  
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();  
        // 得到公钥  
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();  
        try {  
            // 得到公钥字符串  
            String publicKeyString = Base64.encodeToString(publicKey.getEncoded(),true);  
            // 得到私钥字符串  
            String privateKeyString = Base64.encodeToString(privateKey.getEncoded(),true);  
            // 将密钥对写入到文件  
            FileWriter pubfw = new FileWriter(filePath + "/public.key");  
            FileWriter prifw = new FileWriter(filePath + "/private.key");  
            BufferedWriter pubbw = new BufferedWriter(pubfw);  
            BufferedWriter pribw = new BufferedWriter(prifw);  
            pubbw.write(publicKeyString);  
            pribw.write(privateKeyString);  
            pubbw.flush();  
            pubbw.close();  
            pubfw.close();  
            pribw.flush();  
            pribw.close();  
            prifw.close();  
        } catch (Exception e) {  
            e.printStackTrace();  
        }  
    }  

    
    
    /** 
     * 从文件加载公钥 
     * @param file 公钥文件
     */  
    public PublicKey loadPublicKey(File file) { 
    	FileInputStream inputStream = null;
    	try {
    		inputStream = new FileInputStream(file);			
    		return loadPublicKey(inputStream);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("文件不存在");
		}finally {
			try {inputStream.close();} catch (Exception e2) {}
		}
    }
    
    /** 
     * 从文件中输入流中加载公钥 
     * @param in 公钥输入流 
     */  
    public PublicKey loadPublicKey(InputStream in) {  
        try {  
            BufferedReader br= new BufferedReader(new InputStreamReader(in));  
            String readLine= null;  
            StringBuilder sb= new StringBuilder();  
            while((readLine= br.readLine())!=null){  
                if(readLine.charAt(0)=='-'){  
                    continue;  
                }else{  
                    sb.append(readLine);  
                    sb.append('\r');  
                }  
            }  
            byte[] bytes = Base64.decode(sb.toString());
            return loadPublicKey(bytes);  
        } catch (IOException e) {  
            throw new RuntimeException("公钥数据流读取错误");  
        } catch (NullPointerException e) {  
            throw new RuntimeException("公钥输入流为空");  
        } 
    }  
    
    
    /** 
     * 从文件中加载私钥 
     * @param file 私钥文件
     */  
    public PrivateKey loadPrivateKey(File file) { 
    	FileInputStream inputStream = null;
    	try {
    		inputStream = new FileInputStream(file);			
    		return loadPrivateKey(inputStream);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("文件不存在");
		}finally {
			try {inputStream.close();} catch (Exception e2) {}
		}
    }
    
    /** 
     * 从文件中加载私钥 
     * @param keyFileName 私钥文件名 
     * @return 是否成功 
     */  
    public PrivateKey loadPrivateKey(InputStream in) {  
        try {  
            BufferedReader br= new BufferedReader(new InputStreamReader(in));  
            String readLine= null;  
            StringBuilder sb= new StringBuilder();  
            while((readLine= br.readLine())!=null){  
                if(readLine.charAt(0)=='-'){  
                    continue;  
                }else{  
                    sb.append(readLine);  
                    sb.append('\r');  
                }  
            }  
            
            byte[] bytes = Base64.decode(sb.toString());
            
            return loadPrivateKey(bytes);  
        } catch (IOException e) {  
            throw new RuntimeException("私钥数据读取错误");  
        } catch (NullPointerException e) {  
            throw new RuntimeException("私钥输入流为空");  
        } 
        
    } 

    /**
     * 还原公钥，X509EncodedKeySpec 用于构建公钥的规范
     * 
     * @param keyBytes
     * @return
     */
    public static PublicKey loadPublicKey(byte[] keyBytes) {
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(keyBytes);

        try {
            KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
            PublicKey publicKey = factory.generatePublic(x509EncodedKeySpec);
            return publicKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 还原私钥，PKCS8EncodedKeySpec 用于构建私钥的规范
     * 
     * @param keyBytes
     * @return
     */
    public static PrivateKey loadPrivateKey(byte[] keyBytes) {
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(
                keyBytes);
        try {
            KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey privateKey = factory
                    .generatePrivate(pkcs8EncodedKeySpec);
            return privateKey;
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 从KeyStore获取私钥
     * @param location
     * @param alias
     * @param storeType
     * @param storePass
     * @param keyPass
     * @return
     */
    public static PrivateKey loadPrivateKeyFromKeyStore(String location,String alias,String storeType,String storePass,String keyPass){
        try {			
        	storeType = null == storeType ? KeyStore.getDefaultType() : storeType;
        	keyPass = keyPass == null ? storePass : keyPass;
        	KeyStore keyStore = KeyStore.getInstance(storeType);
        	InputStream is = new FileInputStream(location);
        	keyStore.load(is, storePass.toCharArray());
        	// 由密钥库获取密钥的两种方式
        	return (PrivateKey) keyStore.getKey(alias, keyPass.toCharArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
   /**
    * 从KeyStore获取私钥
    * @param location
    * @param alias
    * @param keyStore
    * @param storePass
    * @param keyPass
    * @return
    */
    public static PrivateKey loadPrivateKeyFromKeyStore(KeyStore keyStore,String alias,String keyPass){
        try {			
        	return (PrivateKey) keyStore.getKey(alias, keyPass.toCharArray());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
     * 从KeyStore获取公钥
     * @param location
     * @param alias
     * @param storeType
     * @param storePass
     * @param keyPass
     * @return
     */
    public static PublicKey loadPublicKeyFromKeyStore(String location,String alias,String storeType,String storePass,String keyPass){
        try {			
        	storeType = null == storeType ? KeyStore.getDefaultType() : storeType;
        	keyPass = keyPass == null ? storePass : keyPass;
        	KeyStore keyStore = KeyStore.getInstance(storeType);
        	InputStream is = new FileInputStream(location);
        	keyStore.load(is, storePass.toCharArray());
        	
        	RSAPrivateCrtKey key = (RSAPrivateCrtKey) keyStore.getKey(alias, keyPass.toCharArray());
			RSAPublicKeySpec spec = new RSAPublicKeySpec(key.getModulus(),
					key.getPublicExponent());
			PublicKey publicKey = KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(spec);
            return publicKey;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
    
    /**
     * 
     * @param keyStore
     * @param alias
     * @param keyPass
     * @return
     */
    public static PublicKey loadPublicKeyFromKeyStore(KeyStore keyStore,String alias,String keyPass){
        try {			
        	RSAPrivateCrtKey key = (RSAPrivateCrtKey) keyStore.getKey(alias, keyPass.toCharArray());
			RSAPublicKeySpec spec = new RSAPublicKeySpec(key.getModulus(),
					key.getPublicExponent());
			PublicKey publicKey = KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(spec);
            return publicKey;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
    }
  
  
    /**
     * 从证书文件获取公钥
     * @param certPath
     * @return
     * @throws CertificateException
     * @throws FileNotFoundException
     */
    public static PublicKey loadPublicKeyFromCert(String certPath) throws CertificateException, FileNotFoundException {
    	try {			
    		CertificateFactory cf = CertificateFactory.getInstance("X.509");
    		FileInputStream in = new FileInputStream(certPath);
    		Certificate crt = cf.generateCertificate(in);
    		PublicKey publicKey = crt.getPublicKey();
    		return publicKey;
		} catch (CertificateException e) {
			throw new RuntimeException(e);
		}catch (FileNotFoundException e) {
			throw new RuntimeException("文件不存在");
		}
	}

    /**
     * 加密
     * 
     * @param key
     * @param plainBytes
     * @return
     */
    public static byte[] encrypt(PublicKey key, byte[] plainBytes) {

    	ByteArrayOutputStream out = null;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            
            int inputLen = plainBytes.length;  
            if(inputLen <= MAX_ENCRYPT_BLOCK){
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
            throw new RuntimeException("无此解密算法");  
        } catch (NoSuchPaddingException e) {  
            e.printStackTrace();  
            return null;  
        } catch (InvalidKeyException e) {  
            throw new RuntimeException("解密私钥非法,请检查");  
        } catch (IllegalBlockSizeException e) {  
            throw new RuntimeException("密文长度非法");  
        } catch (BadPaddingException e) {  
            throw new RuntimeException("密文数据已损坏");  
        }finally{
        	try {if(out != null)out.close(); } catch (Exception e2) {}
        }
    }
    
    public static String encrypt(PublicKey key, String plainText){
    	byte[] encodeBytes = encrypt(key, plainText.getBytes(DEFAULT_CHARSET));
		return Base64.encodeToString(encodeBytes,false);
    }

    /**
     * 解密
     * 
     * @param key
     * @param encodedText
     * @return
     */
    public static String decrypt(PrivateKey key, byte[] encodedText) {

    	ByteArrayOutputStream out = null;
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            int inputLen = encodedText.length; 
            
            if(inputLen <= MAX_DECRYPT_BLOCK){
            	return new String(cipher.doFinal(encodedText),DEFAULT_CHARSET);
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
            return new String(out.toByteArray(),DEFAULT_CHARSET);
        } catch (NoSuchAlgorithmException e) {  
            throw new RuntimeException("无此解密算法");  
        } catch (NoSuchPaddingException e) {  
            e.printStackTrace();  
            return null;  
        } catch (InvalidKeyException e) {  
            throw new RuntimeException("解密私钥非法,请检查");  
        } catch (IllegalBlockSizeException e) {  
            throw new RuntimeException("密文长度非法");  
        } catch (BadPaddingException e) {  
            throw new RuntimeException("密文数据已损坏");  
        }finally{
        	try {if(out != null)out.close(); } catch (Exception e2) {}
        }
    }
    
    public static String decrypt(PrivateKey key, String encodedText) {
    	byte[] bytes = Base64.decode(encodedText);
    	return decrypt(key, bytes);
    }
}
