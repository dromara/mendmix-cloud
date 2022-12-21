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
package com.mendmix.common2.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.mendmix.common.util.BeanUtils;
import com.mendmix.common.util.StringConverter;

public class JdbcExecutor {

//	public static String driverClass;
//	public static String url;
//	public static String userName;
//	public static String password;
//
//	static {
//		driverClass = ResourceUtils.getAndValidateProperty("jdbc.driverClassName");
//		url = ResourceUtils.getAndValidateProperty("jdbc.url");
//		userName = ResourceUtils.getAndValidateProperty("jdbc.username");
//		password = ResourceUtils.getAndValidateProperty("jdbc.password");
//		try {Class.forName(driverClass);} catch (ClassNotFoundException e) {}
//	}
	
	private static JdbcExecutor defaultExecutor;
	
	private DataSource dataSource;

	public JdbcExecutor(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public DataSource getDataSource() {
		return dataSource;
	}

	public static JdbcExecutor getDefaultExecutor() {
		if(defaultExecutor != null)return defaultExecutor;
		synchronized (JdbcExecutor.class) {
			if(defaultExecutor != null)return defaultExecutor;
			defaultExecutor = new JdbcExecutor(DataSourceGroups.getDefaultDataSource());
		}
		return defaultExecutor;
	}



	public Connection getconnnection() throws SQLException {
//		Connection con = null;
//		try {
//			con = DriverManager.getConnection(url, userName, password);
//		} catch (SQLException e) {
//			e.printStackTrace();
//		}
		return dataSource.getConnection();
	}
	
	public int insert(String sql, Object[] args) {
		return execute(sql, args);
	}
	
	public int delete(String sql, Object[] args) {
		return execute(sql, args);
	}
	
	public int update(String sql, Object[] args) {
		return execute(sql, args);
	}
	
	/**
	 * @param sql
	 * @param args
	 * @return
	 */
	public int execute(String sql, Object[] args) {
		int result = 0;
		Connection con = null;
		PreparedStatement ps = null;
		try {
			con = getconnnection();
			ps = con.prepareStatement(sql);
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					ps.setObject((i + 1), args[i]);
				}
			}
			result = ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			close(null, ps, con);
		}

		return result;
	}

	/**
	 * Query a single record
	 * 
	 * @param sql
	 * @param args
	 * @return Map<String,Object>
	 */
	public Map<String, Object> queryForMap(String sql, Object[] args) {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, Object>> list = queryForList(sql, args ,true);
		if (list.size() > 0) {
			result = list.get(0);
		}
		return result;
	}

	/**
	 * Query a single record
	 * 
	 * @param sql
	 * @param args
	 * @return <T>
	 */
	public <T> T queryForObject(String sql, Object[] args,Class<T> clazz) {
		T result = BeanUtils.mapToBean(queryForMap(sql, args), clazz);
		return result;
	}

	/**
	 * Query a single record
	 * 
	 * @param sql
	 * @param args
	 * @return List<Map<String,Object>>
	 */
	public List<Map<String, Object>> queryForList(String sql, Object[] args,boolean toCamelCase) {
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			con = getconnnection();
			ps = con.prepareStatement(sql);
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					ps.setObject((i + 1), args[i]);
				}
			}
			rs = ps.executeQuery();
			ResultSetMetaData rsmd = rs.getMetaData();
			int columnCount = rsmd.getColumnCount();
			String key;
			Object value;
			while (rs.next()) {
				Map<String, Object> map = new HashMap<String, Object>();
				for (int i = 1; i <= columnCount; i++) {
					value = rs.getObject(i);
					if(value instanceof LocalDateTime) {
						value = Date.from(((LocalDateTime)value).atZone( ZoneId.systemDefault()).toInstant());
					}
					key = toCamelCase ? StringConverter.toCamelCase(rsmd.getColumnLabel(i)) : rsmd.getColumnLabel(i);
					map.put(key, value);
				}
				result.add(map);
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, ps, con);
		}
		return result;
	}
	
	public <T> List<T> queryForList(String sql, Object[] args,ResultConverter<T> converter) {
		List<T> result = new ArrayList<T>();
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			con = getconnnection();
			ps = con.prepareStatement(sql);
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					ps.setObject((i + 1), args[i]);
				}
			}
			rs = ps.executeQuery();
			while (rs.next()) {
				result.add(converter.convert(rs));
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, ps, con);
		}
		return result;
	}

	/**
	 * Query a single record
	 * 
	 * @param sql
	 * @param args
	 * @return List<T>
	 */
	public <T> List<T> queryForList(String sql, Object[] args, Class<T> clazz) {
		List<Map<String, Object>> mapList = queryForList(sql, args , true);
		List<T> result = new ArrayList<>(mapList.size());
        for (Map<String, Object> map : mapList) {
        	result.add(BeanUtils.mapToBean(map, clazz));
		}
		return result;
	}
	
	public long queryForCount(String sql, Object[] args) {
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			con = getconnnection();
			ps = con.prepareStatement(sql);
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					ps.setObject((i + 1), args[i]);
				}
			}
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getLong(1);
			}
			return 0L;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, ps, con);
		}
	}
	
	public String queryForString(String sql, Object[] args) {
		Connection con = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			con = getconnnection();
			ps = con.prepareStatement(sql);
			if (args != null) {
				for (int i = 0; i < args.length; i++) {
					ps.setObject((i + 1), args[i]);
				}
			}
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getString(1);
			}
			return null;
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			close(rs, ps, con);
		}
	}
	
	public void close(ResultSet rs, Statement st, Connection con) {
		try {if (rs != null)rs.close();} catch (Exception e) {}
		try {if (st != null)st.close();} catch (Exception e) {}
		try {if (con != null)con.close();} catch (Exception e) {}
	}
	
	
}
