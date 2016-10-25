/**
 * 
 */
package com.jeesuite.mybatis.crud.builder;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月25日
 */
public class SqlTemplate {

	public static final String IF_TAG_TEAMPLATE = "<if test=\"%s != null\">%s</if>";
	public static final String SCRIPT_TEMAPLATE = "<script>%s</script>";

	
	public static String wrapIfTag(String fieldName,String expr,boolean skip){
		if(skip)return expr;
		return String.format(IF_TAG_TEAMPLATE, fieldName,expr);
	}
}
