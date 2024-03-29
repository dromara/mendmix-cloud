/*
 * Copyright 2016-2022 www.mendmix.com.
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
package test.excel;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.mendmix.common2.excel.annotation.TitleCell;
import com.mendmix.common2.excel.model.TitleMeta;

/**
 * 个人工资数据
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年11月21日
 */
public class SalaryInfo {

	private int id;
	
	@TitleCell(name="*姓名",column = 1,notNull = true)
	private String name;
	@TitleCell(name="部门",column = 2)
	private String department;
	@TitleCell(name="身份证号",column = 3 )
	private String idCard;
	@TitleCell(name="基本工资",column = 4,row = 2,parentName = "应发工资",type = Float.class)
	private float baseSalary;//基本工资
	@TitleCell(name="岗位工资",column = 5,row = 2,parentName = "应发工资",type = Float.class)
	private float postSalary;//岗位工资
	@TitleCell(name="绩效工资",column = 6,row = 2,parentName = "应发工资",type = Float.class)
	private float performSalary;//绩效工资
	@TitleCell(name="福利津贴",column = 7,row = 2,parentName = "应发工资",type = Float.class)
	private float subsidies;//福利津贴
	@TitleCell(name="扣除金额",column = 8,row = 2,parentName = "应发工资",type = Float.class)
	private float deductSalary; //扣除金额
	@TitleCell(name="*总计",column = 9,row = 2,parentName = "应发工资",notNull = true,type = Float.class)
	private float total;
	@TitleCell(name="*社保基数",column = 10,notNull = true,type = Float.class)
	private float insuranceBase;//社保基数
	@TitleCell(name="*公积金基数",column = 11,notNull = true,type = Float.class)
	private float housefundBase;
	
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
	
	@Override
	public String toString() {
		return "PersonSalaryInfo [id=" + id + ", name=" + name + ", department=" + department + ", idCard=" + idCard
				+ ", baseSalary=" + baseSalary + ", performSalary=" + performSalary + ", postSalary=" + postSalary
				+ ", subsidies=" + subsidies + ", deductSalary=" + deductSalary + ", total=" + total
				+ ", housefundBase=" + housefundBase + ", insuranceBase=" + insuranceBase + "]";
	}
	
	
	public static void main(String[] args) {
		List<TitleMeta> titleCellBeans = new ArrayList<>();
		Field[] fields = SalaryInfo.class.getDeclaredFields();
		
		Map<String, TitleMeta> parentMap = new HashMap<>();
		int index = 0,subIndex = 0;
		for (Field field : fields) {
			if(!field.isAnnotationPresent(TitleCell.class))continue;
			TitleCell annotation = field.getAnnotation(TitleCell.class);
			TitleMeta cell = new TitleMeta(annotation.name());
			
			if(StringUtils.isBlank(annotation.parentName())){
				cell.setColumnIndex(++index);
				titleCellBeans.add(cell);
			}else{
				TitleMeta cellParent = parentMap.get(annotation.parentName());
				if(cellParent == null){
					subIndex = index;
					cellParent = new TitleMeta(annotation.parentName());
					cellParent.setColumnIndex(++index);
					parentMap.put(annotation.parentName(), cellParent);
					titleCellBeans.add(cellParent);
				}
				cell.setColumnIndex(++subIndex);
				cell.setRowIndex(1);
				cellParent.addChildren(cell);
			}
		}
		
		for (TitleMeta cell : titleCellBeans) {
			System.out.println(cell);
			if(cell.getChildren().size() > 0){
				for (TitleMeta child : cell.getChildren()) {
					System.out.println("--" + child);
					
				}
			}
		}
	}
	
}
