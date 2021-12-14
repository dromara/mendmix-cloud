package com.jeesuite.mybatis.plugin.pagination;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.model.PageParams;
import com.jeesuite.mybatis.datasource.DatabaseType;

public class PageSqlUtils {
	
	private static final String REGEX_N_T_S = "\\n+|\\t+\\s{2,}";

	private static final String PAGE_SIZE_PLACEHOLDER = "#{pageSize}";

	private static final String OFFSET_PLACEHOLDER = "#{offset}";

	private static final String SQL_SELECT_PATTERN = "(select|SELECT).*?(?=from|FROM)";
	
	private static final String SQL_ORDER_PATTERN = "(order|ORDER)\\s+(by|BY)";

	private static final String SQL_COUNT_PREFIX = "SELECT count(1) ";
	
	private static final String[] UNION_KEYS = new String[] {" union "," UNION "};
	
	private static final String COMMON_COUNT_SQL_TEMPLATE = "SELECT count(1) FROM (%s) tmp";
	
	private static Map<String, String>  pageTemplates = new HashMap<>(4);
	
	static {
		pageTemplates.put(DatabaseType.mysql.name(), "%s limit #{offset},#{pageSize}");
		pageTemplates.put(DatabaseType.oracle.name(), "select * from (select a1.*,rownum rn from (%s) a1 where rownum <=#{offset} + #{pageSize}) where rn>=#{offset}");
		pageTemplates.put(DatabaseType.postgresql.name(), "%s limit #{pageSize} offset #{offset}");
		pageTemplates.put(DatabaseType.h2.name(), "%s limit #{pageSize} offset #{offset}");
	}
	
	public static String getLimitSQL(DatabaseType dbType,String sql){
		return String.format(pageTemplates.get(dbType.name()), sql);
	}
	
	public static String getLimitSQL(DatabaseType dbType,String sql,PageParams pageParams){
		return getLimitSQL(dbType, sql)//
				.replace(OFFSET_PLACEHOLDER, String.valueOf(pageParams.offset()))//
				.replace(PAGE_SIZE_PLACEHOLDER, String.valueOf(pageParams.getPageSize()))//
				.replaceAll(REGEX_N_T_S, StringUtils.SPACE);
	}
	
	public static String getCountSql(String sql){
		sql = sql.replaceAll(REGEX_N_T_S, StringUtils.SPACE).split(SQL_ORDER_PATTERN)[0];
		if(StringUtils.containsAny(sql, UNION_KEYS)) {
			sql = String.format(COMMON_COUNT_SQL_TEMPLATE, sql);
		}else {
			sql = sql.replaceFirst(SQL_SELECT_PATTERN, SQL_COUNT_PREFIX);
		}
		return sql;
	}
	
	public static void main(String[] args) {
		String sql = "select a.* from audited_policy a where 1=1 \n\t   \n\t   \n\t   \n\t   \n\t   and title like CONCAT('%',?,'%')   \n\t   \n\t   \n\t   \n\t   \n\t   \n\t   \n\t    \n\t   \n\t  order by updated_at desc";
	
		System.out.println(">>>>" +getCountSql(sql));
		System.out.println(">>>>" +getLimitSQL(DatabaseType.mysql, sql, new PageParams()));
		
		System.out.println("------------");
		sql = "select a.* from audited_policy a where 1=1 \n and EXISTS (select 1 from policy_tag tag where tag.id in (select tag_id from policy_r_tag rtag where rtag.policy_id= a.id) and tag.name like CONCAT('%',?,'%') ) order by updated_at desc";
	
		System.out.println(">>>>" +getCountSql(sql));
		System.out.println(">>>>" +getLimitSQL(DatabaseType.mysql, sql, new PageParams()));
		
		sql = "SELECT  u.id as userId,u.name,u.cert_no as idcard,u.mobile,u.verified as userVerified,u.cert_front_url certFrontUrl,u.cert_back_url certBackUrl,  o.salary,o.notax as afterTaxSalary,o.vattax as vatax,o.extratax as surtax,o.incometax,o.tax,o.invfphm as invoiceNo  FROM cbd_invorderdet";
	    
		System.out.println(sql.replaceAll("(select|SELECT).*?(?=from|FROM)", SQL_COUNT_PREFIX));
	}
}
