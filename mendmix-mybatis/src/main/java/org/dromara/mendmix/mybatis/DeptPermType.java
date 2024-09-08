package org.dromara.mendmix.mybatis;

/**
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年2月28日
 */
public enum DeptPermType {
   /**全部*/_ALL_,
   /**无部门权限仅个人*/none,
   /**当前部门*/current,
   /**当前及子部门*/currentAndSub,
   /**指定部门*/specified,
   /**指定及子部门*/specifiedAndSub
}
