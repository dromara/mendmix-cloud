package com.jeesuite.common.model;

import java.util.ArrayList;
import java.util.List;

public class PageParams {

	//当前页
    private int pageNo = 1;
    //每页的数量
    private int pageSize = 10;
    
    private List<OrderBy> orderBys;
    
	public PageParams() {}

	public PageParams(int pageNo, int pageSize) {
		super();
		this.pageNo = pageNo;
		this.pageSize = pageSize;
	}
	
	public PageParams(int pageNo, int pageSize,OrderBy orderBy) {
		super();
		this.pageNo = pageNo;
		this.pageSize = pageSize;
		orderBy(orderBy);
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
	
    
	public List<OrderBy> getOrderBys() {
		return orderBys;
	}

	public void setOrderBys(List<OrderBy> orderBys) {
		this.orderBys = orderBys;
	}
	
	public PageParams orderBy(OrderBy orderBy) {
		if(orderBy == null)return this;
		if(this.orderBys == null)this.orderBys = new ArrayList<>(2);
		this.orderBys.add(orderBy);
		return this;
	}
	
	public PageParams orderBys(OrderBy...orderBys) {
		if(this.orderBys == null)this.orderBys = new ArrayList<>(orderBys.length);
		for (OrderBy order : orderBys) {
			if(order != null)this.orderBys.add(order);
		}
		return this;
	}

	public int offset() {
		return (pageNo - 1) * pageSize;
	}

	@Override
	public String toString() {
		return "PageParams [pageNo=" + pageNo + ", pageSize=" + pageSize + "]";
	}
	
	
}
