/**
 * 
 */
package test.excel;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.RandomUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import com.jeesuite.common2.excel.ExcelPerfModeReader;
import com.jeesuite.common2.excel.ExcelWriter;
import com.jeesuite.common2.excel.helper.ExcelBeanHelper;
import com.jeesuite.common2.excel.model.ExcelMeta;
import com.jeesuite.common2.excel.model.TitleMeta;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月3日
 */
public class ExcelTest {

	/**
	 * @param args
	 * @throws IOException
	 * @throws InvalidFormatException
	 */
	public static void main(String[] args) throws InvalidFormatException, IOException {

		List<PersonSalaryInfo> list = new ExcelPerfModeReader("/Users/ayg/Desktop/工资模板.xls")
				.read(PersonSalaryInfo.class);

		System.out.println(list);

		ExcelWriter writer = new ExcelWriter("/Users/ayg/Desktop/test" + RandomUtils.nextInt(1000, 99999) + ".xlsx");
		writer.write(list, PersonSalaryInfo.class);
		writer.close();

//		ExcelMeta excelMeta = ExcelBeanHelper.getExcelMeta(PersonSalaryInfo.class);
//		// 写标题
//		for (int i = 1; i <= excelMeta.getTitleRowNum(); i++) {
//			for (int j = 1; j <= excelMeta.getTitleColumnNum(); j++) {
//				TitleMeta titleMeta = excelMeta.getTitleMeta(i, j);
//				System.out.println(i + "-" + j + "-" + (titleMeta == null ? "" : titleMeta.getTitle()));
//			}
//		}
	}

}
