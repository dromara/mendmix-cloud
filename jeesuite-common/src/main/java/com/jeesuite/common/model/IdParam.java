package com.jeesuite.common.model;

/**
 * 
 * <br>
 * Class Name   : IdParam
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2019年7月12日
 */
public class IdParam<T> {

	private T id;
	
	public IdParam() {}
	
	/**
	 * @param id
	 */
	public IdParam(T id) {
		this.id = id;
	}

	/**
	 * @return the id
	 */
	public T getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(T id) {
		this.id = id;
	}

}
