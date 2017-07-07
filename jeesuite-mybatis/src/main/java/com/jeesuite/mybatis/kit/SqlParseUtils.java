package com.jeesuite.mybatis.kit;

import java.util.List;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.parser.MySqlStatementParser;
import com.alibaba.druid.sql.dialect.mysql.visitor.MySqlSchemaStatVisitor;

public class SqlParseUtils {

	public static void main(String[] args) {
		String sql = "DELETE a1, a2 FROM t1 AS a1 INNER JOIN t2 AS a2 WHERE a1.id=a2.id;";
		 
		MySqlStatementParser parser = new MySqlStatementParser(sql);
		List<SQLStatement> statementList = parser.parseStatementList();
		SQLStatement statemen = statementList.get(0);
		 
		MySqlSchemaStatVisitor visitor = new MySqlSchemaStatVisitor();
		statemen.accept(visitor);
		
		System.out.println(visitor.getTables());
		
		System.out.println(visitor.getColumns());
		
		System.out.println(visitor.getConditions());
	}
}
