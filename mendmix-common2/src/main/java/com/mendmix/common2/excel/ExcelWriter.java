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
package com.mendmix.common2.excel;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mendmix.common2.excel.helper.ExcelBeanHelper;
import com.mendmix.common2.excel.model.ExcelMeta;
import com.mendmix.common2.excel.model.TitleMeta;



public final class ExcelWriter implements Closeable {

    private static final Logger     LOG    = LoggerFactory.getLogger(ExcelWriter.class);

    private       String   sheetName;
    private       OutputStream outputStream;
    private final SXSSFWorkbook workbook;

    /**
     * 构造方法，传入需要操作的excel文件路径
     *
     * @param excelFilePath 需要操作的excel文件的路径
     * @throws IOException            IO流异常
     * @throws InvalidFormatException 非法的格式异常
     */
    public ExcelWriter(String excelFilePath,String sheetName) throws IOException, InvalidFormatException {
    	this.sheetName = sheetName;
    	File file = new File(excelFilePath);
    	boolean exists = file.exists();
		if(!exists)file.createNewFile();
    	outputStream = new FileOutputStream(file);
        this.workbook = createWorkbook(exists ? file : null);
    }
    
    public ExcelWriter(String excelFilePath) throws IOException, InvalidFormatException {
    	this(excelFilePath,"Sheet1");
    }
    
    public ExcelWriter(OutputStream outputStream) throws IOException, InvalidFormatException {
    	this(outputStream,"Sheet1");
    }

    public ExcelWriter(OutputStream outputStream, String sheetName) throws InvalidFormatException, IOException {
		super();
		this.outputStream = outputStream;
		this.sheetName = sheetName;
		this.workbook = createWorkbook(null);
	}

	/**
     * 设置需要读取的sheet名字，不设置默认的名字是Sheet1，也就是excel默认给的名字，所以如果文件没有自已修改，这个方法也就不用调了
     *
     * @param sheetName 需要读取的Sheet名字
     */
    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    private SXSSFWorkbook createWorkbook(File existFile) throws IOException, InvalidFormatException {
    	SXSSFWorkbook workbook;
        if (existFile == null) {
            workbook = new SXSSFWorkbook(1000);//内存中保留 1000条数据，以免内存溢出
        } else {
        	workbook = new SXSSFWorkbook(new XSSFWorkbook(existFile), 1000);
        }
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
		
		ExcelMeta excelMeta = ExcelBeanHelper.getExcelMeta(clazz);
		try {
			Sheet sheet = workbook.createSheet(this.sheetName);
			sheet.setDefaultColumnWidth(15); 
			CellStyle titleStyle = workbook.createCellStyle(); 
			titleStyle.setAlignment(HorizontalAlignment.CENTER);
			titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
			titleStyle.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
			titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			titleStyle.setBorderBottom(BorderStyle.THIN); //下边框
			titleStyle.setBorderLeft(BorderStyle.THIN);
			Font font = workbook.createFont();
			font.setFontName("宋体");
			font.setFontHeightInPoints((short) 13);
			titleStyle.setFont(font);
			//列值类型
			Class<?>[] cellValueTypes = new Class<?>[excelMeta.getTitleColumnNum()];
			//写标题
			for (int i = 1; i <= excelMeta.getTitleRowNum(); i++) {
				Row excelRow = sheet.createRow(i - 1);
				for (int j = 1; j <= excelMeta.getTitleColumnNum(); j++) {
					TitleMeta titleMeta = excelMeta.getTitleMeta(i, j);
					Cell cell = excelRow.createCell(j - 1);
					cell.setCellValue(titleMeta == null ? "" : titleMeta.getTitle());
					cell.setCellStyle(titleStyle);
					cellValueTypes[j-1] = titleMeta.getValueType();
				}
			}
			//合并表头
            //sheet.addMergedRegion(new CellRangeAddress(0, 0, 3, 8));
			//sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, 0));
			mergeColumns(sheet,titleStyle);
			mergeRows(sheet,titleStyle,excelMeta);
			
			// 行数
			int rowsCount = sheet.getPhysicalNumberOfRows();
			//冻结表头
			//sheet.createFreezePane(0, rowsCount - 1);
			
			// 写入内容
			List<Object[]> rows = ExcelBeanHelper.beanToExcelValueArrays(list, clazz);
	        // 列数
	        //int colsCount = sheet.getRow(0).getPhysicalNumberOfCells();
			for (int i = 0; i < rows.size(); i++) {
				Row excelRow = sheet.createRow(i + rowsCount);
				
				Object[] vals = rows.get(i);
				for (int j = 0; j < vals.length; j++) {
					Cell cell = excelRow.createCell(j);
					if(cellValueTypes[j] == int.class || cellValueTypes[j] == Integer.class){
					    cell.setCellValue(vals[j] == null ? 0f : Integer.parseInt(vals[j].toString()));
					}else if(cellValueTypes[j] == float.class || cellValueTypes[j] == Float.class 
							|| cellValueTypes[j] == double.class || cellValueTypes[j] == Double.class
							|| cellValueTypes[j] == BigDecimal.class){
					    cell.setCellValue(vals[j] == null ? 0d : Double.parseDouble(vals[j].toString()));
					}else{
						cell.setCellValue(vals[j] == null ? "" : vals[j].toString());
					}
				}
			}
			workbook.write(outputStream);
			return true;
		} catch (IOException e) {
			LOG.error("流异常", e);
		} catch (Exception e) {
			LOG.error("其他异常", e);
		} finally {
		} 
		return false;
	}

	
	 /**
     * 合并列
     */
    private void mergeColumns(Sheet sheet,CellStyle cellStyle) {
        // 行数
        int rowsCount = sheet.getPhysicalNumberOfRows();
        // 列数
        int colsCount = sheet.getRow(0).getPhysicalNumberOfCells();
 
        Row row = null;
        Cell cell1 = null;
        Cell cell2 = null;
 
        int colSpan = 0;
 
        for (int r = 0; r < rowsCount; r++) {
            row = sheet.getRow(r);
            // 重置
            colSpan = 0;
            row = sheet.getRow(r);
            for (int c = 0; c < colsCount; c++) {
                cell1 = row.getCell(c);
                cell2 = row.getCell(c + 1);
                if (cell1 == null) {// 如果当前单元格是空的，跳过，继续当前行的后一个单元格查找
                    if (c == colsCount - 1) {
                        break;
                    } else {
                        continue;
                    }
                }
                if (cell2 == null) {// 说明当前行已经到最后一个单元格了
                    if (colSpan >= 1) {// 判断colSpan是否大于等于1，大于1就要合并了
                        // 合并行中连续相同的值的单元格
                        sheet.addMergedRegion(new CellRangeAddress(r, r, c - colSpan, c));
                        break;
                    }
                }
                if (cell1 != null && cell2 != null) {
                    // 如果当前单元格和下一个单元格内容相同，那么colSpan加1
                    if (cell1.getStringCellValue().equals(cell2.getStringCellValue())) {
                        colSpan++;
                    } else {
                        // 如果当前单元格和下一个不等，那么判断colSpan是否大于等于1
                        if (colSpan >= 1) {
                            // 合并行中连续相同的值的单元格
                            sheet.addMergedRegion(new CellRangeAddress(r, r, c - colSpan, c));
                            Cell nowCell = sheet.getRow(r).getCell(c - colSpan);
                    		nowCell.setCellStyle(cellStyle);
                            // 合并后重置colSpan
                            colSpan = 0;
                            continue;
                        }
                    }
                }
 
            }
        }
 
    }
 
    /**
     * 合并行
     */
    //TODO 暂时支持两行表头
    private void mergeRows(Sheet sheet,CellStyle cellStyle,ExcelMeta excelMeta) {
    	
    	Row row = null;
    	Cell cell = null;
    	String[] lastRowVals = new String[excelMeta.getTitleColumnNum()];
    	for (int r = 0; r < excelMeta.getTitleRowNum(); r++) {
			for (int c = 0; c < excelMeta.getTitleColumnNum(); c++) {
				row = sheet.getRow(r);
                cell = row.getCell(c);
                if(r == 0){
                	lastRowVals[c] = cell.getStringCellValue();
                }else{                	
                	if(StringUtils.equals(lastRowVals[c],cell.getStringCellValue())){
                		cell.setCellValue("");
                		sheet.addMergedRegion(new CellRangeAddress(0, r, c, c));
                		Cell nowCell = sheet.getRow(0).getCell(c);
                		nowCell.setCellStyle(cellStyle);
                	}
                }
                
			}
		}
    
    }
   

    @Override
    public void close() throws IOException {
    	try {this.outputStream.close();} catch (Exception e) {}
    	try {this.workbook.close();} catch (Exception e) {}
    }
}
