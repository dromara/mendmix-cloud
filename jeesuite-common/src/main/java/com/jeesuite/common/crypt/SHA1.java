package com.jeesuite.common.crypt;

import java.security.MessageDigest;
import java.util.Arrays;

import com.jeesuite.common.JeesuiteBaseException;


public class SHA1 {

	 /**
     * <p>
     * 用SHA1算法生成安全签名
     * </p>
     *
     * @param token
     * 				票据
     * @param timestamp
     * 				时间戳
     * @param nonce
     * 				随机字符串
     * @param encrypt
     * 				密文
     * @return 安全签名
     *
     * @throws AESException {@link AESException}
     */
    public static String getSHA1(String token, String timestamp, String nonce, String encrypt)  {
        try {
            String[] array = new String[]{token, timestamp, nonce, encrypt};
            StringBuffer sb = new StringBuffer();

			/* 字符串排序 */
            Arrays.sort(array);
            for (int i = 0; i < 4; i++) {
                sb.append(array[i]);
            }
			
			/* SHA1签名生成 */
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            md.update(sb.toString().getBytes());
            byte[] digest = md.digest();

            StringBuffer hexstr = new StringBuffer();
            String shaHex = "";
            for (int i = 0; i < digest.length; i++) {
                shaHex = Integer.toHexString(digest[i] & 0xFF);
                if (shaHex.length() < 2) {
                    hexstr.append(0);
                }
                hexstr.append(shaHex);
            }

            return hexstr.toString();
        } catch (Exception e) {
            throw new JeesuiteBaseException(500, "error", e);
        }
    }
}
