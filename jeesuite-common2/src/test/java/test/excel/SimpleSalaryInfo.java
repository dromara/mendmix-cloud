/**
 * 
 */
package test.excel;

import com.jeesuite.common2.excel.annotation.TitleCell;

/**
 * 个人工资数据
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月21日
 */
public class SimpleSalaryInfo {

	// 姓名 身份证号 手机 税后工资
	@TitleCell(name = "姓名", column = 1)
	private String name;
	@TitleCell(name = "身份证号", column = 2)
	private String idCard;
	@TitleCell(name = "手机", column = 3)
	private String mobileNo;// 社保基数
	@TitleCell(name = "税后工资", column = 4, notNull = true, type = Float.class)
	private float salary;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIdCard() {
		return idCard;
	}

	public void setIdCard(String idCard) {
		this.idCard = idCard;
	}

	public String getMobileNo() {
		return mobileNo;
	}

	public void setMobileNo(String mobileNo) {
		this.mobileNo = mobileNo;
	}

	public float getSalary() {
		return salary;
	}

	public void setSalary(float salary) {
		this.salary = salary;
	}

}
