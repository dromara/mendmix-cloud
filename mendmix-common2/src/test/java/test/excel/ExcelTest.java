/*
 * Copyright 2016-2022 dromara.org.
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

import java.io.IOException;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import org.dromara.mendmix.common.util.JsonUtils;
import org.dromara.mendmix.common.excel.ExcelPerfModeReader;

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
		//普通方式读取
		String excelFilePath = "/Users/jiangwei/Desktop/invorderdet_template.xlsx";

		//大文件读取防止内存溢出
		List<SimpleSalaryInfo> list = new ExcelPerfModeReader(excelFilePath).read(SimpleSalaryInfo.class);

		System.out.println(JsonUtils.toPrettyJson(list));

	}

}
