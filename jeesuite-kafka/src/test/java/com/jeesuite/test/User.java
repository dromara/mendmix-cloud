/**
 * 
 */
package com.jeesuite.test;

import java.io.Serializable;
import java.util.Date;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月3日
 */
public class User implements Serializable {

	private int id;
	
	private int age;
	
	private String name;
	
	private String nickName;
	
	private int gender;
	
	private String password;
	
	private String idcard;
	
	private Date createdAt;
	
	private Date updatedAt;
	
}
