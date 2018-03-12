package com.jeesuite.mybatis.plugin.pagination;

public class PageParams {

	//当前页
    private int pageNo = 1;
    //每页的数量
    private int pageSize = 10;
    
    
	public PageParams() {}

	public PageParams(int pageNo, int pageSize) {
		super();
		this.pageNo = pageNo;
		this.pageSize = pageSize;
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
    
	public int offset() {
		return (pageNo - 1) * pageSize;
	}

	@Override
	public String toString() {
		return "PageParams [pageNo=" + pageNo + ", pageSize=" + pageSize + "]";
	}
	
	
}
