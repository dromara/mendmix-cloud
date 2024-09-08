/**
 * 
 */
package org.dromara.mendmix.mybatis.plugin;

import java.util.concurrent.Callable;

import org.dromara.mendmix.common.CurrentRuntimeContext;
import org.dromara.mendmix.common.CustomRequestHeaders;
import org.dromara.mendmix.common.ThreadLocalContext;
import org.dromara.mendmix.mybatis.MybatisRuntimeContext;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年12月12日
 */
public class OnceInterceptorStrategy {
	
	private String originTenantId;
	private boolean originIgnoreAny;
	private boolean originIgnoreTenant;
	private boolean originIgnoreDataPermission;
	private boolean originIgnoreSoftDeleteConditon;
	private boolean originIgnoreTableSharding;
	private boolean originIgnoreSqlRewrite;
	private boolean originIgnoreChangeLog;
	private boolean originIgnoreRwRoute;

	private OnceInterceptorStrategy() {}

	public static OnceInterceptorStrategy apply() {
		final OnceInterceptorStrategy strategy = new OnceInterceptorStrategy();
		strategy.originTenantId = CurrentRuntimeContext.getTenantId();
		strategy.originIgnoreAny = MybatisRuntimeContext.isIgnoreAny();
		strategy.originIgnoreTenant = MybatisRuntimeContext.isIgnoreTenantMode();
		strategy.originIgnoreDataPermission = MybatisRuntimeContext.isIgnoreDataPermission();
		strategy.originIgnoreSoftDeleteConditon = MybatisRuntimeContext.isIgnoreSoftDeleteConditon();
		strategy.originIgnoreTableSharding = MybatisRuntimeContext.isIgnoreTableSharding();
		strategy.originIgnoreSqlRewrite = MybatisRuntimeContext.isIgnoreSqlRewrite();
		strategy.originIgnoreChangeLog = MybatisRuntimeContext.isIgnoreLoggingDataChange();
		strategy.originIgnoreRwRoute = MybatisRuntimeContext.isIgnoreRwRoute();
		return strategy;
	}
	
	public OnceInterceptorStrategy ignoreAny() {
		MybatisRuntimeContext.setIgnoreAny(true);
		return this;
	}
	
	public OnceInterceptorStrategy ignoreRwRoute() {
		MybatisRuntimeContext.setIgnoreRwRoute(true);
		return this;
	}
	
	public OnceInterceptorStrategy ignoreTenant() {
		MybatisRuntimeContext.setIgnoreTenant(true);
		return this;
	}
	
	public OnceInterceptorStrategy ignoreDataPermission() {
		MybatisRuntimeContext.setIgnoreDataPermission(true);
		return this;
	}
	
	public OnceInterceptorStrategy ignoreSoftDeleteConditon() {
		MybatisRuntimeContext.setIgnoreSoftDeleteConditon(true);
		return this;
	}
	
	public OnceInterceptorStrategy ignoreTableSharding() {
		MybatisRuntimeContext.setIgnoreTableSharding(true);
		return this;
	}
	
	public OnceInterceptorStrategy ignoreSqlRewrite() {
		MybatisRuntimeContext.setIgnoreSqlRewrite(true);
		return this;
	}
	
	public OnceInterceptorStrategy ignoreLoggingDataChange() {
		MybatisRuntimeContext.setIgnoreLoggingDataChange(true);
		return this;
	}
	
	public OnceInterceptorStrategy usingTenant(String tenantId) {
		CurrentRuntimeContext.setTenantId(tenantId);
		return this;
	}
	
	public <V> V exec(Callable<V> caller) {
		boolean originIgnoreCache = MybatisRuntimeContext.isIgnoreCache();
		MybatisRuntimeContext.setIgnoreCache(true);
		try {
			return caller.call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}finally {
			ThreadLocalContext.set(CustomRequestHeaders.HEADER_TENANT_ID, originTenantId);
			MybatisRuntimeContext.setIgnoreAny(originIgnoreAny);
			MybatisRuntimeContext.setIgnoreCache(originIgnoreCache);
			MybatisRuntimeContext.setIgnoreTenant(originIgnoreTenant);
			MybatisRuntimeContext.setIgnoreDataPermission(originIgnoreDataPermission);
			MybatisRuntimeContext.setIgnoreSoftDeleteConditon(originIgnoreSoftDeleteConditon);
			MybatisRuntimeContext.setIgnoreSqlRewrite(originIgnoreSqlRewrite);
			MybatisRuntimeContext.setIgnoreTableSharding(originIgnoreTableSharding);
			MybatisRuntimeContext.setIgnoreLoggingDataChange(originIgnoreChangeLog);
			MybatisRuntimeContext.setIgnoreRwRoute(originIgnoreRwRoute);
		}
	}
}
