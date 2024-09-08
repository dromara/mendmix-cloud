/*
 * Copyright 2016-2022 dromara.org.
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
package org.dromara.mendmix.common.model;

import java.util.ArrayList;
import java.util.List;

public class PageParams {

	//当前页
    private int pageNo = 1;
    //每页的数量
    private int pageSize = 10;
    
    private List<OrderBy> orderBys;
    
    private boolean concurrency;
    
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
	
	
	public boolean isConcurrency() {
		return concurrency;
	}

	public void setConcurrency(boolean concurrency) {
		this.concurrency = concurrency;
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
