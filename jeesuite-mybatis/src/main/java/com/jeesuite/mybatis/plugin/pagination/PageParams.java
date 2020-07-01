package com.jeesuite.mybatis.plugin.pagination;

import com.jeesuite.mybatis.MybatisConfigs;

public class PageParams {

	private static int maxPageSizeLimit = MybatisConfigs.getPaginationMaxLimit();
	//当前页
    private int pageNo = 1;
    //每页的数量
    private int pageSize = 10;
    
    
	public PageParams() {}

	public PageParams(int pageNo, int pageSize) {
		setPageNo(pageNo);
		setPageSize(pageSize);
	}
	
	public int getPageNo() {
		return pageNo;
	}
	public void setPageNo(int pageNo) {
		if(pageNo > 0){
			this.pageNo = pageNo;
		}
		
	}
	
	public int getPageSize() {
		return pageSize;
	}
	
	public void setPageSize(int pageSize) {
		if(maxPageSizeLimit > 0 && pageSize > maxPageSizeLimit){
			this.pageSize = maxPageSizeLimit;
		}else if(pageSize > 0){			
			this.pageSize = pageSize;
		}
	}
    
	public int offset() {
		return (pageNo - 1) * pageSize;
	}

	@Override
	public String toString() {
		return "PageParams [pageNo=" + pageNo + ", pageSize=" + pageSize + "]";
	}
	
	
}
