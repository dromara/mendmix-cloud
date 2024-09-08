/**
 * 
 */
package org.dromara.mendmix.springweb.exporter.metrics;

/**
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年8月18日
 */
public class CompHealthMetrics {

	private String server;
	private HealthState status;
	private UsageInfo pool;
	
	public CompHealthMetrics() {}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public HealthState getStatus() {
		return status;
	}
	
	public void setStatus(HealthState status) {
		this.status = status;
	}
	
	public UsageInfo getPool() {
		return pool;
	}

	public void setPool(UsageInfo pool) {
		this.pool = pool;
	}


}
