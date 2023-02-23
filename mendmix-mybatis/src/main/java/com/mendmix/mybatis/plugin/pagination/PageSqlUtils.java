/*
 * Copyright 2016-2022 www.mendmix.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mendmix.mybatis.plugin.pagination;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common.model.PageParams;
import com.mendmix.mybatis.datasource.DatabaseType;

public class PageSqlUtils {

	private static final String KEY_FROM = "FROM";
	private static final String KEY_SELECT = "SELECT";
	private static final char PAIR_CLOSE_CHAR = ')';
	private static final char PAIR_OPEN_CHAR = '(';
	private static final String[] SQL_LINE_CHARS = new String[] { "\r", "\n", "\t" };
	private static final String[] SQL_LINE_REPLACE_CHARS = new String[] { " ", " ", " " };

	private static final String PAGE_SIZE_PLACEHOLDER = "#{pageSize}";

	private static final String OFFSET_PLACEHOLDER = "#{offset}";

	private static final Pattern SQL_SELECT_PATTERN = Pattern.compile("(SELECT\\s{1})|(\\s{1}FROM\\s{1})",Pattern.CASE_INSENSITIVE);
	private static final Pattern SQL_ORDER_PATTERN = Pattern.compile("\\s{1}ORDER\\s+BY\\s{1}",Pattern.CASE_INSENSITIVE);

	private static final String SQL_COUNT_PREFIX = "SELECT count(1) ";

	private static String[] unionKeys = new String[] { " UNION ", " union " };
	//
	private static Pattern groupByPattern = Pattern.compile("\\s+GROUP\\s+BY\\s+",Pattern.CASE_INSENSITIVE);
	private static List<Pattern> aggregationKeyPatterns = Arrays.asList(
			Pattern.compile("(\\s+|,)(COUNT|MIN|MAX|SUM|AVG)\\(",Pattern.CASE_INSENSITIVE),	
			Pattern.compile("(\\s+|,)DISTINCT",Pattern.CASE_INSENSITIVE)
	);

	private static Pattern nestSelectPattern = Pattern.compile("\\(\\s{0,}(select|SELECT)\\s+");

	private static Map<String, String> pageTemplates = new HashMap<>(4);

	private static String commonCountSqlTemplate = "select count(1) from (%s) tmp";
	static {
		pageTemplates.put(DatabaseType.mysql.name(), "%s limit #{offset},#{pageSize}");
		pageTemplates.put(DatabaseType.oracle.name(),
				"select * from (select a1.*,rownum rn from (%s) a1 where rownum <=#{offset} + #{pageSize}) where rn>=#{offset}");
		pageTemplates.put(DatabaseType.postgresql.name(), "%s limit #{pageSize} offset #{offset}");
		pageTemplates.put(DatabaseType.h2.name(), "%s limit #{pageSize} offset #{offset}");
	}

	public static String getLimitSQL(DatabaseType dbType, String sql) {
		return String.format(pageTemplates.get(dbType.name()), sql);
	}

	public static String getLimitSQL(DatabaseType dbType, String sql, PageParams pageParams) {
		return getLimitSQL(dbType, sql)//
				.replace(OFFSET_PLACEHOLDER, String.valueOf(pageParams.offset()))//
				.replace(PAGE_SIZE_PLACEHOLDER, String.valueOf(pageParams.getPageSize()));
	}

	public static String getCountSql(String sql){
		final String formatSql = StringUtils.replaceEach(sql, SQL_LINE_CHARS, SQL_LINE_REPLACE_CHARS).trim();
		String selectHead = matchTopSelectFrom(formatSql);
		//最外层包含聚合查询
		boolean useWrapperMode = aggregationKeyPatterns.stream().anyMatch(p -> p.matcher(selectHead).find());
		String outterSql = formatSql;
		if(!useWrapperMode) {
			String removeSelectHead = formatSql.substring(selectHead.length());
			//嵌套查询
			if(nestSelectPattern.matcher(formatSql).find()) {
				String innerSql = matchOutterParenthesesPair(removeSelectHead);
				outterSql = formatSql.replace(innerSql, StringUtils.EMPTY);
			}
			//最外层sql包含union或者group by
			useWrapperMode = StringUtils.containsAny(outterSql, unionKeys) 
					|| groupByPattern.matcher(outterSql).find();
			
		}
		if(useWrapperMode) {
			return String.format(commonCountSqlTemplate, formatSql);
		}else {
			Matcher matcher = SQL_ORDER_PATTERN.matcher(outterSql);
			if(matcher.find()) {
				int end = formatSql.lastIndexOf(matcher.group());
				sql = formatSql.substring(0, end);
			}else {
				sql = formatSql;
			}
			return StringUtils.replaceOnce(sql, selectHead, SQL_COUNT_PREFIX);
		}
	}
	
	private static String matchOutterParenthesesPair(String content) {
		char[] chars = content.toCharArray();
        int start = -1;
        int end  = -1;
        int matchIndex = 0;
        for (int i = 0; i < chars.length; i++) {
        	if(chars[i] == PAIR_OPEN_CHAR) {
        		if(start < 0) {
        			start = i;
        		}
        		matchIndex++;
        	}else if(chars[i] == PAIR_CLOSE_CHAR) {
        		matchIndex--;
        		if(matchIndex == 0) {
        			end = i;
        			break;
        		}
        	}
		}
        return content.substring(start,end + 1);
	}
	
	private static String matchTopSelectFrom(String sql) {
		int start = -1;
        int end  = -1;
        int matchIndex = 0;
		Matcher matcher = SQL_SELECT_PATTERN.matcher(sql);
		while (matcher.find()) {
			if(matcher.group().trim().equalsIgnoreCase(KEY_SELECT)) {
				matchIndex++;
				if(start < 0) {
        			start = matcher.start();
        		}
			}else if(matcher.group().trim().equalsIgnoreCase(KEY_FROM)) {
				matchIndex--;
        		if(matchIndex == 0) {
        			end = matcher.end();
        			break;
        		}
			}
		}
		//5 = from + 一个空格
		return sql.substring(start, end - 5);
		
	}

	public static void main(String[] args) throws IOException {
		String sql = "select order_from from audited_policy a where 1=1\nand title like CONCAT('%',?,'%') order by updated_at desc";
		// sql = FileUtils.readFileToString(new File("D:\\datas\\1.txt"),
		// StandardCharsets.UTF_8);
		System.out.println(">>>>" + getCountSql(sql));
		//System.out.println(">>>>" + getLimitSQL(DatabaseType.mysql, sql, new PageParams()));
	}
}
