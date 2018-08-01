/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jeesuite.common.util;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年4月12日
 */
public class IdCardFormatVerifyHelper {

	/** 大陆地区地域编码最大值 **/
	private static final int          MAX_MAINLAND_AREACODE     = 659004;
    /** 大陆地区地域编码最小值 **/
    private static final int          MIN_MAINLAND_AREACODE     = 110000;
    /** 香港地域编码值 **/
    private static final int          HONGKONG_AREACODE         = 810000;                                                                                                                                                 // 香港地域编码值
    /** 台湾地域编码值 **/
    private static final int          TAIWAN_AREACODE           = 710000;
    /** 澳门地域编码值 **/
    private static final int          MACAO_AREACODE            = 820000;
    /** 闰年生日正则 **/
    private static final Pattern       regexBirthdayInLeapYear   = Pattern.compile("^((19[0-9]{2})|(200[0-9])|(201[0-5]))((01|03|05|07|08|10|12)(0[1-9]|[1-2][0-9]|3[0-1])|(04|06|09|11)(0[1-9]|[1-2][0-9]|30)|02(0[1-9]|[1-2][0-9]))$");
    /** 平年生日正则 **/
    private static final Pattern       regexBirthdayInCommonYear = Pattern.compile("^((19[0-9]{2})|(200[0-9])|(201[0-5]))((01|03|05|07|08|10|12)(0[1-9]|[1-2][0-9]|3[0-1])|(04|06|09|11)(0[1-9]|[1-2][0-9]|30)|02(0[1-9]|1[0-9]|2[0-8]))$");

    private static final Pattern      byteRegex = Pattern.compile("^([0-9]{17}[0-9Xx])|([0-9]{15})$");
    
    private static String[] ValCodeArr = { "1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2" };
    private static String[] Wi = { "7", "9", "10", "5", "8", "4", "2", "1", "6", "3", "7", "9", "10", "5", "8", "4", "2" };


    /**
     * 身份证格式校验
     * @param idNumber
     * @return
     */
    public static final boolean verifyFormat(String idNumber) {
        if (StringUtils.isBlank(idNumber)) {
            return false;
        }
        idNumber = idNumber.trim();
        if (!checkIdNumberRegex(idNumber)) {
            return false;
        }
        if (!checkIdNumberArea(idNumber.substring(0, 6))) {
            return false;
        }
        idNumber = convertFifteenToEighteen(idNumber);
        if (!checkBirthday(idNumber.substring(6, 14))) {
            return false;
        }
        if (!checkIdNumberVerifyCode(idNumber)) {
            return false;
        }
        return true;
    }

    /**
     * 身份证正则校验
     */
    private static boolean checkIdNumberRegex(String idNumber) {
    	return byteRegex.matcher(idNumber).matches();
    }

    /**
     * 身份证地区码检查
     */
    private static boolean checkIdNumberArea(String idNumberArea) {
        int areaCode = Integer.parseInt(idNumberArea);
        if (areaCode == HONGKONG_AREACODE || areaCode == MACAO_AREACODE || areaCode == TAIWAN_AREACODE) {
            return true;
        }
        if (areaCode <= MAX_MAINLAND_AREACODE && areaCode >= MIN_MAINLAND_AREACODE) {
            return true;
        }
        return false;
    }

    /**
     * 将15位身份证转换为18位
     */
    private static String convertFifteenToEighteen(String idNumber) {
        if (15 != idNumber.length()) {
            return idNumber;
        }
        idNumber = idNumber.substring(0, 6) + "19" + idNumber.substring(6, 15);
        idNumber = idNumber + getVerifyCode(idNumber);
        return idNumber;
    }

    /**
     * 根据身份证前17位计算身份证校验码
     */
    private static String getVerifyCode(String idNumber) {
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum = sum + Integer.parseInt(String.valueOf(idNumber.charAt(i))) * Integer.parseInt(Wi[i]);
        }
        return ValCodeArr[sum % 11];
    }

    /**
     * 身份证出生日期嘛检查
     */
    private static boolean checkBirthday(String idNumberBirthdayStr) {
        Integer year = null;
        try {
            year = Integer.valueOf(idNumberBirthdayStr.substring(0, 4));
        } catch (Exception e) {
        }
        if (null == year) {
            return false;
        }
        if (isLeapYear(year)) {
        	return regexBirthdayInLeapYear.matcher(idNumberBirthdayStr).matches();
        } else {
            return regexBirthdayInCommonYear.matcher(idNumberBirthdayStr).matches();
        }
    }

    /**
     * 判断是否为闰年
     */
    private static boolean isLeapYear(int year) {
        return (year % 400 == 0) || (year % 100 != 0 && year % 4 == 0);
    }

    /**
     * 身份证校验码检查
     */
    private static boolean checkIdNumberVerifyCode(String idNumber) {
        return getVerifyCode(idNumber).equalsIgnoreCase(idNumber.substring(17));
    }

    public static void main(String[] args) {
        System.out.println(verifyFormat("433130198401163211"));
    }
}
