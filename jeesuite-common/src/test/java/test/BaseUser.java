/**
 * 
 */
package test;

import java.io.Serializable;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月4日
 */
public class BaseUser implements Serializable {

	private static final long serialVersionUID = 1L;

	private Integer id;

	private String name;

	private String mobile = "13800138000";

	private String email;
	
	private BaseUser father;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getMobile() {
		return mobile;
	}

	public void setMobile(String mobile) {
		this.mobile = mobile;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public BaseUser getFather() {
		return father;
	}

	public void setFather(BaseUser father) {
		this.father = father;
	}

   
}
