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
		String[] segments = sql.split(SQL_SPLIT_PATTERN);
		return SQL_COUNT_PREFIX.concat(segments[1]);
	}
	
	public static void main(String[] args) {
		String sql = "SELECT * FROM user u LEFT OUTER JOIN account a ON u.id = a.user_id WHERE a.name is NULL order by create_at desc";
		String[] strings = sql.split(SQL_SPLIT_PATTERN);
		for (String string : strings) {
			System.out.println("----->" + string);
		}
	}
}
