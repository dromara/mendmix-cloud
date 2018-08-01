package com.jeesuite.common.util;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * 格式校验工具
 */
public class FormatValidateUtils {  
	
    //------------------常量定义  
    /** 
     * Email正则表达式;
     */
	public static final String EMAIL = "^\\w+((-|\\.)\\w+)*@\\w+(\\.[a-zA-Z]{2,3})+$";
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL);
    /** 
     * 电话号码正则表达式
     */
    public static final String PHONE = "(^(\\d{2,4}[-_－—]?)?\\d{3,8}([-_－—]?\\d{3,8})?([-_－—]?\\d{1,7})?$)|(^0?1[35]\\d{9}$)" ;  
    private static final Pattern PHONE_PATTERN = Pattern.compile(PHONE);
    /** 
     * 手机号码正则表达式
     */
    public static final String MOBILE ="^(1(3|5|7|8)[0-9])\\d{8}$";  
    private static final Pattern MOBILE_PATTERN = Pattern.compile(MOBILE);
    /** 
     * Integer正则表达式 ^-?(([1-9]\d*$)|0) 
     */
    public static final String  INTEGER = "^-?(([1-9]\\d*$)|0)";  
    private static final Pattern INTEGER_PATTERN = Pattern.compile(INTEGER);
    /** 
     * 正整数正则表达式 >=0 ^[1-9]\d*|0$ 
     */
    public static final String  INTEGER_NEGATIVE = "^[1-9]\\d*|0$";  
    private static final Pattern INTEGER_NEGATIVE_PATTERN = Pattern.compile(INTEGER_NEGATIVE);
    /** 
     * 负整数正则表达式 <=0 ^-[1-9]\d*|0$ 
     */
    public static final String  INTEGER_POSITIVE = "^-[1-9]\\d*|0$";  
    private static final Pattern INTEGER_POSITIVE_PATTERN = Pattern.compile(INTEGER_POSITIVE);
    /** 
     * Double正则表达式 ^-?([1-9]\d*\.\d*|0\.\d*[1-9]\d*|0?\.0+|0)$ 
     */
    public static final String  DOUBLE ="^-?([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0)$";  
    private static final Pattern DOUBLE_PATTERN = Pattern.compile(DOUBLE);
    /** 
     * 正Double正则表达式 >=0  ^[1-9]\d*\.\d*|0\.\d*[1-9]\d*|0?\.0+|0$　 
     */
    public static final String  DOUBLE_NEGATIVE ="^[1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|0?\\.0+|0$";  
    private static final Pattern DOUBLE_NEGATIVE_PATTERN = Pattern.compile(DOUBLE_NEGATIVE);
    /** 
     * 负Double正则表达式 <= 0  ^(-([1-9]\d*\.\d*|0\.\d*[1-9]\d*))|0?\.0+|0$ 
     */
    public static final String  DOUBLE_POSITIVE ="^(-([1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*))|0?\\.0+|0$"; 
    private static final Pattern DOUBLE_POSITIVE_PATTERN = Pattern.compile(DOUBLE_POSITIVE);
    /** 
     * 年龄正则表达式 ^(?:[1-9][0-9]?|1[01][0-9]|120)$ 匹配0-120岁 
     */
    public static final String  AGE="^(?:[1-9][0-9]?|1[01][0-9]|120)$";  
    private static final Pattern AGE_PATTERN = Pattern.compile(AGE);
    /** 
     * 邮编正则表达式  [0-9]\d{5}(?!\d) 国内6位邮编 
     */
    public static final String  ZIP_CODE="[0-9]\\d{5}(?!\\d)";    
    private static final Pattern ZIP_CODE_PATTERN = Pattern.compile(ZIP_CODE);
    /** 
     * 匹配由26个英文字母组成的字符串  ^[A-Za-z]+$ 
     */
    public static final String STR_ENG="^[A-Za-z]+$";  
    private static final Pattern STR_ENG_PATTERN = Pattern.compile(STR_ENG);

    /** 
     * URL正则表达式 
      * 匹配 http www ftp 
     */
    public static final String URL = "^(http|www|ftp|)?(://)?(\\w+(-\\w+)*)(\\.(\\w+(-\\w+)*))*((:\\d+)?)(/(\\w+(-\\w+)*))*(\\.?(\\w)*)(\\?)?" +  
                                    "(((\\w*%)*(\\w*\\?)*(\\w*:)*(\\w*\\+)*(\\w*\\.)*(\\w*&)*(\\w*-)*(\\w*=)*(\\w*%)*(\\w*\\?)*" +  
                                    "(\\w*:)*(\\w*\\+)*(\\w*\\.)*" +  
                                    "(\\w*&)*(\\w*-)*(\\w*=)*)*(\\w*)*)$";  
    private static final Pattern URL_PATTERN = Pattern.compile(URL);

        
    /** 
     * 判断字段是否为Email 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isEmail(String str) {  
        return EMAIL_PATTERN.matcher(str).matches();
    }  
    /** 
     * 判断是否为电话号码 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isPhone(String str) {  
        return PHONE_PATTERN.matcher(str).matches(); 
    }  
    /** 
     * 判断是否为手机号码 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isMobile(String str) {  
        return MOBILE_PATTERN.matcher(str).matches();
    }  
    /** 
     * 判断是否为Url 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isUrl(String str) {  
        return URL_PATTERN.matcher(str).matches();
    }     
 
    /** 
     * 判断字段是否为INTEGER  符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isInteger(String str) {  
        return INTEGER_PATTERN.matcher(str).matches(); 
    }  
    /** 
     * 判断字段是否为正整数正则表达式 >=0 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isNegativeInteger(String str) {  
        return INTEGER_NEGATIVE_PATTERN.matcher(str).matches();  
    }  
    /** 
     * 判断字段是否为负整数正则表达式 <=0 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isPositiveInteger(String str) {  
        return INTEGER_POSITIVE_PATTERN.matcher(str).matches();  
    }     
    /** 
     * 判断字段是否为DOUBLE 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isDouble(String str) {  
        return DOUBLE_PATTERN.matcher(str).matches();  
    }  
    /**  
     * 判断字段是否为正浮点数正则表达式 >=0 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isNegativeDouble(String str) {  
        return DOUBLE_NEGATIVE_PATTERN.matcher(str).matches();   
    }  
    /** 
     * 判断字段是否为负浮点数正则表达式 <=0 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isPositiveDouble(String str) {  
        return DOUBLE_POSITIVE_PATTERN.matcher(str).matches();  
    }     
    /** 
     * 判断字段是否为日期 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isDate(String str) {  
    	try {
    		DateUtils.parseDate(str);
    		return true;
		} catch (Exception e) {
			return false;
		}
    }  
 
    /** 
     * 判断字段是否为年龄 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isAge(String str) {  
        return AGE_PATTERN.matcher(str).matches();   
    }  

    /** 
     * 判断字段是否为身份证 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isIdCard(String str) {
    	return IdCardFormatVerifyHelper.verifyFormat(str);
    }  
    /** 
     * 判断字段是否为邮编 符合返回ture 
     * @param str 
     * @return boolean 
     */
    public static  boolean isZipCode(String str) {  
        return ZIP_CODE_PATTERN.matcher(str).matches();    
    }  
    /** 
     * 判断字符串是不是全部是英文字母 
     * @param str 
     * @return boolean 
     */
    public static boolean isEnglish(String str) {  
        return STR_ENG_PATTERN.matcher(str).matches();    
    }  
 
     
    /** 
     * 判断字符串是不是数字组成 
     * @param str 
     * @return boolean 
     */
    public static boolean isNumber(String str) {  
        return StringUtils.isNumeric(str);  
    } 

    
    public static void main(String[] args) {
		
    	System.out.println(isMobile("17012341234"));
    	System.out.println(isEmail("jia-ng.s_ky@qqq1111111111.com.cn"));
    	System.out.println(isEmail("jian_g-sk&y@qq.com"));
	}
     
} 
