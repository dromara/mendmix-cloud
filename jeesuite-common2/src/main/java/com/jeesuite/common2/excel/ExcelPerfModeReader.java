package com.jeesuite.common2.excel;

import java.io.IOException;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.filesystem.NotOLE2FileException;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;

import com.jeesuite.common2.excel.convert.XLS2CSV;
import com.jeesuite.common2.excel.convert.XLSX2CSV;
import com.jeesuite.common2.excel.helper.ExcelBeanHelper;
import com.jeesuite.common2.excel.helper.ExcelValidator;

/**
 * 性能模式读取工具（适合大数据量读取，解决内存消耗过高的问题）
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月18日
 */
public class ExcelPerfModeReader {

    private final String   excelFilePath;
    private int titleStartAt = 1;
    
    public ExcelPerfModeReader(String excelFilePath) {
		this.excelFilePath = excelFilePath;
	}
  
    public ExcelPerfModeReader titleStartAt(int start){
    	this.titleStartAt = start;
    	return this;
    }

	private List<String> read(){

		List<String> result = null;
		if(excelFilePath.toLowerCase().endsWith(ExcelValidator.XLS_SIFFIX)){
			try {
				result = readAsXLS(excelFilePath);
			} catch (OfficeXmlFileException e) {
				result = readAsXLSX(excelFilePath);
			}
		}else{
			try {
				result = readAsXLSX(excelFilePath);
			} catch (OLE2NotOfficeXmlFileException e) {
				result =  readAsXLS(excelFilePath);
			}
		}
		
		removeLineBeforeTitle(result);
		return result;
	}
	
	public <T> List<T> read(Class<T> clazz){
		List<String> rows = read();
		if(rows == null || rows.size() <= 1)throw new ExcelOperBaseException("记录不存在");
		return ExcelBeanHelper.setRowValues(clazz, rows);
	} 
	
	
//	public <T> void read(Class<T> clazz,ResultProcessor<T> processor){
//	   
//	} 
	
	private List<String> readAsXLS(String path){
		try {				
			XLS2CSV xls2csv = new XLS2CSV(path, -1);
			return xls2csv.process();
		} catch (Exception e) {
			if(e instanceof NotOLE2FileException || e instanceof NotOfficeXmlFileException || e instanceof OfficeXmlFileException){
				throw new ExcelOperBaseException("请选择正确格式excel文件");
			}
			if(e instanceof IOException){
				throw new ExcelOperBaseException("文件读取失败");
			}
			throw new RuntimeException(e);
		}
	}
	
	
	private List<String> readAsXLSX(String path){
		OPCPackage opcPackage = null;
		try {
			opcPackage = OPCPackage.open(path, PackageAccess.READ);
			XLSX2CSV xlsx2csv = new XLSX2CSV(opcPackage, System.out, -1);
			return xlsx2csv.process();
		} catch (Exception e) {
			if(e instanceof OLE2NotOfficeXmlFileException || e instanceof NotOLE2FileException || e instanceof NotOfficeXmlFileException || e instanceof OfficeXmlFileException){
				throw new ExcelOperBaseException("请选择正确格式excel文件");
			}
			if(e instanceof IOException){
				throw new ExcelOperBaseException("文件读取失败");
			}
			if(e instanceof InvalidOperationException){
				throw new ExcelOperBaseException(e);
			}
			throw new RuntimeException(e);
		}finally{
			try {opcPackage.close();} catch (Exception e) {}
		}
	}
	
	private void removeLineBeforeTitle(List<String> lines){
		//第一行固定为sheet名
		if(titleStartAt == 1 || lines == null || lines.size() - 1 <= titleStartAt)return;
		for (int i = 1; i < titleStartAt; i++) {
			lines.remove(1);
		}
	}
	
}
