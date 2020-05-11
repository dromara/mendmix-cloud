/**
 * 
 */
package com.jeesuite.mybatis.crud;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年10月25日
 */
public class SqlTemplate {

	public static final String IF_TAG_TEAMPLATE = "<if test=\"%s != null\">%s,</if>";
	public static final String SCRIPT_TEMAPLATE = "<script>%s</script>";
	public static final String TRIM_PREFIX = "<trim prefix=\"(\" suffix=\")\" suffixOverrides=\",\">\n";
	public static final String TRIM_SUFFIX = "</trim>";
	
	public static final String INSERT = "INSERT INTO %s \n %s \n VALUES \n %s";
	public static final String UPDATE_BY_KEY = "UPDATE %s %s \n WHERE %s = #{%s}";
	public static final String BATCH_INSERT = "INSERT INTO %s \n %s \n VALUES \n <foreach collection=\"list\" item=\"item\" index=\"index\" separator=\",\">%s</foreach>";
	public static final String SELECT_BY_KEYS = "SELECT * FROM %s WHERE %s IN  <foreach collection=\"list\" item=\"id\" index=\"index\" open=\"(\" close=\")\" separator=\",\">#{id}</foreach>";
	
	public static String wrapIfTag(String fieldName,String expr,boolean skip){
		if(skip)return expr;
		return String.format(IF_TAG_TEAMPLATE, fieldName,expr);
	}
}
