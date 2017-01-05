/**
 * 
 */
package test.excel;

import java.util.List;

import com.jeesuite.common2.excel.convert.ExcelConvertCSVReader;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月3日
 */
public class ExcelTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		List<PersonSalaryInfo> list = ExcelConvertCSVReader.read("/Users/ayg/Desktop/工资模板.xls", PersonSalaryInfo.class);
		
		System.out.println(list);
	}

}
