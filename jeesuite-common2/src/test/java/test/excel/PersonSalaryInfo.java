/**
 * 
 */
package test.excel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.json.JsonUtils;
import com.jeesuite.common2.excel.annotation.TitleCell;
import com.jeesuite.common2.excel.model.TitleCellBean;

/**
 * 个人工资数据
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月21日
 */
public class PersonSalaryInfo {

	private int id;
	
	@TitleCell(name="*姓名",index = 0,notNull = true)
	private String name;
	@TitleCell(name="部门",index = 1)
	private String department;
	@TitleCell(name="身份证号",index = 2 )
	private String idCard;
	@TitleCell(name="基本工资",index = 3,parentIndex = 3,parentName = "应发工资")
	private float baseSalary;//基本工资
	@TitleCell(name="绩效工资",index = 4,parentIndex = 3,parentName = "应发工资")
	private float performSalary;//绩效工资
	@TitleCell(name="岗位工资",index = 5,parentIndex = 3,parentName = "应发工资")
	private float postSalary;//岗位工资
	@TitleCell(name="*福利津贴",index = 6,parentIndex = 3,parentName = "应发工资")
	private float subsidies;//福利津贴
	@TitleCell(name="扣除金额",index = 7,parentIndex = 3,parentName = "应发工资")
	private float deductSalary; //扣除金额
	@TitleCell(name="*总计",index = 8,parentIndex = 3,parentName = "应发工资",notNull = true)
	private float total;
	@TitleCell(name="*公积金基数",index = 9,notNull = true)
	private float housefundBase;
	@TitleCell(name="*社保基数",index = 10,notNull = true)
	private float insuranceBase;//社保基数
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDepartment() {
		return department;
	}
	public void setDepartment(String department) {
		this.department = department;
	}
	public String getIdCard() {
		return idCard;
	}
	public void setIdCard(String idCard) {
		this.idCard = idCard;
	}
	public float getBaseSalary() {
		return baseSalary;
	}
	public void setBaseSalary(float baseSalary) {
		this.baseSalary = baseSalary;
	}
	public float getPerformSalary() {
		return performSalary;
	}
	public void setPerformSalary(float performSalary) {
		this.performSalary = performSalary;
	}
	public float getPostSalary() {
		return postSalary;
	}
	public void setPostSalary(float postSalary) {
		this.postSalary = postSalary;
	}
	public float getSubsidies() {
		return subsidies;
	}
	public void setSubsidies(float subsidies) {
		this.subsidies = subsidies;
	}
	
	public float getDeductSalary() {
		return deductSalary;
	}
	public void setDeductSalary(float deductSalary) {
		this.deductSalary = deductSalary;
	}
	public float getTotal() {
		return total;
	}
	public void setTotal(float total) {
		this.total = total;
	}
	public float getHousefundBase() {
		return housefundBase;
	}
	public void setHousefundBase(float housefundBase) {
		this.housefundBase = housefundBase;
	}
	public float getInsuranceBase() {
		return insuranceBase;
	}
	public void setInsuranceBase(float insuranceBase) {
		this.insuranceBase = insuranceBase;
	}	
	
	public static void main(String[] args) {
		List<TitleCellBean> titleCellBeans = new ArrayList<>();
		Field[] fields = PersonSalaryInfo.class.getDeclaredFields();
		
		Map<String, TitleCellBean> parentMap = new HashMap<>();
		int index = 0,subIndex = 0;
		for (Field field : fields) {
			if(!field.isAnnotationPresent(TitleCell.class))continue;
			TitleCell annotation = field.getAnnotation(TitleCell.class);
			TitleCellBean cell = new TitleCellBean(annotation.name());
			
			if(StringUtils.isBlank(annotation.parentName())){
				cell.setColumnIndex(++index);
				titleCellBeans.add(cell);
			}else{
				TitleCellBean cellParent = parentMap.get(annotation.parentName());
				if(cellParent == null){
					subIndex = index;
					cellParent = new TitleCellBean(annotation.parentName());
					cellParent.setColumnIndex(++index);
					parentMap.put(annotation.parentName(), cellParent);
					titleCellBeans.add(cellParent);
				}
				cell.setColumnIndex(++subIndex);
				cell.setRowIndex(1);
				cellParent.addChildren(cell);
			}
		}
		
		for (TitleCellBean cell : titleCellBeans) {
			System.out.println(cell);
			if(cell.getChildren().size() > 0){
				for (TitleCellBean child : cell.getChildren()) {
					System.out.println("--" + child);
					
				}
			}
		}
	}
	
}
