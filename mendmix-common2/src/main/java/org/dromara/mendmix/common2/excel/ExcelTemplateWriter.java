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
package org.dromara.mendmix.common.excel;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.common.excel.helper.ExcelBeanHelper;



public final class ExcelTemplateWriter implements Closeable {

    private static final Logger     LOG    = LoggerFactory.getLogger(ExcelTemplateWriter.class);

    private       String templatePath;
    private       OutputStream outputStream;
    private final SXSSFWorkbook workbook;

    /**
     * 构造方法，传入需要操作的excel文件路径
     *
     * @param outputPath 需要操作的excel文件的路径
     * @throws IOException            IO流异常
     * @throws InvalidFormatException 非法的格式异常
     */
    public ExcelTemplateWriter(String templatePath,String outputPath) throws IOException, InvalidFormatException {
    	this.templatePath = templatePath;
    	File file = new File(outputPath);
    	boolean exists = file.exists();
		if(!exists)file.createNewFile();
    	outputStream = new FileOutputStream(file);
        this.workbook = createWorkbook();
    }
  

    public ExcelTemplateWriter(String templatePath,OutputStream outputStream) throws InvalidFormatException, IOException {
		super();
		this.templatePath = templatePath;
		this.outputStream = outputStream;
		this.workbook = createWorkbook();
	}


    private SXSSFWorkbook createWorkbook() throws IOException, InvalidFormatException {
    	File templateFile = new File(templatePath);
    	if(!templateFile.exists())throw new FileNotFoundException("Template["+templatePath + "] not found");
    	SXSSFWorkbook workbook = new SXSSFWorkbook(new XSSFWorkbook(templateFile), 1000);
        return workbook;
    }
    
    /**
     * 将数据写入excel文件
     *
     * @param list 数据列表
     * @param <T>  泛型
     * @return 写入结果
     */
	public <T> boolean write(List<T> list, Class<T> clazz) {
		
		List<Object[]> rows = ExcelBeanHelper.beanToExcelValueArrays(list, clazz);
		try {
			Sheet sheet = workbook.getSheetAt(0);
			for (int i = 2; i < rows.size(); i++) {
				Row excelRow = sheet.getRow(i);
				Object[] vals = rows.get(i);
				for (int j = 0; j < vals.length; j++) {
					Cell cell = excelRow.getCell(j);
					cell.setCellValue(vals[j] == null ? "" : vals[j].toString());
				}
			}
			workbook.write(outputStream);
			return true;
		} catch (IOException e) {
			LOG.error("流异常", e);
		} catch (Exception e) {
			LOG.error("其他异常", e);
		} 
		return false;
	}



    @Override
    public void close() throws IOException {
    	try {this.outputStream.close();} catch (Exception e) {}
    	try {this.workbook.close();} catch (Exception e) {}
    }
}
