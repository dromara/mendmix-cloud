package com.jeesuite.common2.excel;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jeesuite.common2.excel.helper.ExcelBeanHelper;



public final class ExcelWriter implements Closeable {

    private static final Logger     LOG    = LoggerFactory.getLogger(ExcelWriter.class);

    private       String   sheetName;
    private final String   excelFilePath;
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
        this.excelFilePath = excelFilePath;
        this.workbook = createWorkbook();
    }
    
    public ExcelWriter(String excelFilePath) throws IOException, InvalidFormatException {
    	this(excelFilePath,"Sheet1");
    }

    /**
     * 设置需要读取的sheet名字，不设置默认的名字是Sheet1，也就是excel默认给的名字，所以如果文件没有自已修改，这个方法也就不用调了
     *
     * @param sheetName 需要读取的Sheet名字
     */
    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    private SXSSFWorkbook createWorkbook() throws IOException, InvalidFormatException {
    	SXSSFWorkbook workbook;
        File file = new File(this.excelFilePath);
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new IOException("文件创建失败");
            }
            workbook = new SXSSFWorkbook(1000);//内存中保留 1000条数据，以免内存溢出
        } else {
        	workbook = new SXSSFWorkbook(new XSSFWorkbook(file), 1000);
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
	public <T> boolean export(List<T> list, Class<T> clazz) {
		if (null == this.excelFilePath || "".equals(this.excelFilePath))
			throw new NullPointerException("excelFilePath is null");
		FileOutputStream fileOutputStream = null;
		List<Object[]> rows = ExcelBeanHelper.beanToExcelValueArrays(list, clazz);
		try {
			Sheet sheet = workbook.createSheet(this.sheetName);
			//sheet.autoSizeColumn(1, true);
			sheet.setDefaultColumnWidth(15); 
			for (int i = 0; i < rows.size(); i++) {
				Row excelRow = sheet.createRow(i);
				
				Object[] vals = rows.get(i);
				for (int j = 0; j < vals.length; j++) {
					Cell cell = excelRow.createCell(j);
					cell.setCellValue(vals[j] == null ? "" : vals[j].toString());
				}
			}
			File file = new File(this.excelFilePath);
			if (!file.exists()) {
				if (!file.createNewFile()) {
					throw new IOException("文件创建失败");
				}
			}
			fileOutputStream = new FileOutputStream(file);
			workbook.write(fileOutputStream);
			return true;
		} catch (IOException e) {
			LOG.error("流异常", e);
		} catch (Exception e) {
			LOG.error("其他异常", e);
		} finally {
			if (null != fileOutputStream) {
				try {
					fileOutputStream.close();
				} catch (IOException e) {
					LOG.error("关闭流异常", e);
				}
			}
		}

		return false;
	}



    @Override
    public void close() throws IOException {
        this.workbook.close();
    }
}
