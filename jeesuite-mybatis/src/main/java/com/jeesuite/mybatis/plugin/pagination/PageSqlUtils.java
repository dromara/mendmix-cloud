package com.jeesuite.mybatis.plugin.pagination;

import org.apache.commons.lang3.StringUtils;

public class PageSqlUtils {
	
	private static final String REGEX_N_T_S = "\\n+|\\t+\\s{2,}";

	private static final String PAGE_SIZE_PLACEHOLDER = "#{pageSize}";

	private static final String OFFSET_PLACEHOLDER = "#{offset}";

	private static final String SQL_SELECT_PATTERN = "(select|SELECT).*?(?=f|F)";
	
	private static final String SQL_ORDER_PATTERN = "(order|ORDER)\\s+(by|BY)";

	private static final String SQL_COUNT_PREFIX = "SELECT count(1) ";
	
	
	public static enum DbType{
		MYSQL("%s limit #{offset},#{pageSize}"),
		ORACLE("select * from (select a1.*,rownum rn from (%s) a1 where rownum <=#{offset} + #{pageSize}) where rn>=#{offset}"),
		H2("%s limit #{pageSize} offset #{offset}"),
		POSTGRESQL("%s limit #{pageSize} offset #{offset}");
		
		private final String template;

		private DbType(String template) {
			this.template = template;
		}

		public String getTemplate() {
			return template;
		}
		
		
	}
	
	public static String getLimitSQL(DbType dbType,String sql){
		return String.format(dbType.getTemplate(), sql);
	}
	
	public static String getLimitSQL(DbType dbType,String sql,PageParams pageParams){
		return getLimitSQL(dbType, sql)//
				.replace(OFFSET_PLACEHOLDER, String.valueOf(pageParams.getOffset()))//
				.replace(PAGE_SIZE_PLACEHOLDER, String.valueOf(pageParams.getPageSize()))//
				.replaceAll(REGEX_N_T_S, StringUtils.SPACE);
	}
	
	public static String getCountSql(String sql){
		sql = sql.replaceAll(REGEX_N_T_S, StringUtils.SPACE).split(SQL_ORDER_PATTERN)[0];
		return sql.replaceFirst(SQL_SELECT_PATTERN, SQL_COUNT_PREFIX);
	}
	
	public static void main(String[] args) {
		String sql = "select a.* from audited_policy a where 1=1 \n\t   \n\t   \n\t   \n\t   \n\t   and title like CONCAT('%',?,'%')   \n\t   \n\t   \n\t   \n\t   \n\t   \n\t   \n\t    \n\t   \n\t  order by updated_at desc";
	
		System.out.println(">>>>" +getCountSql(sql));
		System.out.println(">>>>" +getLimitSQL(DbType.MYSQL, sql, new PageParams()));
		
		System.out.println("------------");
		sql = "select a.* from audited_policy a where 1=1 \n and EXISTS (select 1 from policy_tag tag where tag.id in (select tag_id from policy_r_tag rtag where rtag.policy_id= a.id) and tag.name like CONCAT('%',?,'%') ) order by updated_at desc";
	
		System.out.println(">>>>" +getCountSql(sql));
		System.out.println(">>>>" +getLimitSQL(DbType.MYSQL, sql, new PageParams()));
	}
}
