package com.jeesuite.common.util;

import com.jeesuite.common.JeesuiteBaseException;

public class AssertUtil {

	public static void isTrue(boolean expression, int code,String message) {
		if (!expression) {
			throw new JeesuiteBaseException(code, message);
		}
	}
	

	public static void isNull(Object object, String message) {
		if (object != null) {
			throw new JeesuiteBaseException(ResourceUtils.getInt("errorcode:record.existed", 500), message);
		}
	}

	public static void isNull(Object object) {
		isNull(object, "记录已存在");
	}

	public static void notNull(Object object, String message) {
		if (object == null) {
			throw new JeesuiteBaseException(ResourceUtils.getInt("errorcode:record.not-exist", 500), message);
		}
	}

	public static void notNull(Object object) {
		notNull(object, "记录不存在");
	}
	

	public static void notBlank(String expression, String message) {
		if (expression == null ||"".equals(expression.trim())) {
			throw new JeesuiteBaseException(ResourceUtils.getInt("errorcode:param.required", 400), message);
		}
	}

	public static void notBlank(String expression){
		 notBlank(expression, "参数不能为空");
	}

}
