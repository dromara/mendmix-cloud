package com.jeesuite.mybatis.plugin.pagination;

public class PageSqlUtils {
	
	private static final String SQL_SPLIT_PATTERN = "(select|SELECT).*(FROM|from)|(order|ORDER)\\s+(by|BY)";

	private static final String SQL_COUNT_PREFIX = "SELECT count(1) FROM ";
	
	public static enum DbType{
		MYSQL("%s limit #{offset},#{pageSize}"),
		ORACLE("select * from ( select row__.*, rownum rownum__ from ( %s ) row__ where rownum <=  %s ) where rownum__ > %s"),
		H2("%s limit %s offset %s"),
		POSTGRESQL("%s limit %s offset %s");
		
		private final String template;

		private DbType(String template) {
			this.template = template;
		}

		public String getTemplate() {
			return template;
		}
		
		
	}
	
	public static String getLimitString(DbType dbType,String sql){
		return String.format(dbType.getTemplate(), sql);
	}
	
	public static String getCountSql(String sql){
		String[] segments = sql.replaceAll("\\n+", " ").split(SQL_SPLIT_PATTERN);
		return SQL_COUNT_PREFIX.concat(segments[1]).replaceAll("\\s{2,}", " ");
	}
	
	public static void main(String[] args) {
		String sql = "SELECT   id,user_id,user_name,source,title,languege_id,framework_id,like_count,comment_count,view_count,is_recommend,is_original,tags,created_at FROM sc_posts where 1=1  \n\n\n\n and is_recommend = 1 order by created_at desc";
		String[] strings = sql.split(SQL_SPLIT_PATTERN);
		for (String string : strings) {
			System.out.println("----->" + string);
		}
		
		System.out.println(">>>>" +getCountSql(sql));
	}
}
