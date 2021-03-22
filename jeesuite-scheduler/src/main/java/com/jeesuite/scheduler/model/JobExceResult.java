package com.jeesuite.scheduler.model;

import java.util.Date;

/**
 * 
 * 
 * <br>
 * Class Name   : JobExceResult
 *
 * @author jiangwei
 * @version 1.0.0
 * @date Mar 21, 2021
 */
public class JobExceResult {

	private boolean result;
	private Date time;
	
	public boolean isResult() {
		return result;
	}
	public void setResult(boolean result) {
		this.result = result;
	}
	public Date getTime() {
		return time;
	}
	public void setTime(Date time) {
		this.time = time;
	}
	
	
}
