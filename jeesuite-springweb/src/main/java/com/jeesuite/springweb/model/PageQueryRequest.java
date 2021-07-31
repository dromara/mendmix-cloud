package com.jeesuite.springweb.model;

import com.jeesuite.common.model.PageParams;

/**
 * 
 * <br>
 * Class Name   : PageQueryRequest
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2017年3月23日
 */
public class PageQueryRequest<T> {

	private int pageNo = 1;
	private int pageSize = 15;
	private OrderBy orderBy;
	private T example;
	
	public PageQueryRequest() {}
	
	public PageQueryRequest(int pageNo, int pageSize, OrderBy orderBy, T example) {
		super();
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.orderBy = orderBy;
		this.example = example;
	}

	public PageQueryRequest(int pageNo, int pageSize, T example) {
		super();
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		this.example = example;
	}

	public int getPageNo() {
		return pageNo;
	}
	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}
	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	public T getExample() {
		return example;
	}
	public void setExample(T example) {
		this.example = example;
	}
	public OrderBy getOrderBy() {
		return orderBy;
	}
	public void setOrderBy(OrderBy orderBy) {
		this.orderBy = orderBy;
	}
	
	public PageParams asPageParam() {
		return new PageParams(pageNo <= 0 ? 1 : pageNo, pageSize);
	}

}
