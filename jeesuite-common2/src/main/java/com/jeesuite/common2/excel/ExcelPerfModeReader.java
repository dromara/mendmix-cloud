package com.jeesuite.common2.excel;

import java.io.IOException;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
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
    
    
    public ExcelPerfModeReader(String excelFilePath) {
		super();
		this.excelFilePath = excelFilePath;
	}
  

	private List<String> read(){

		if(excelFilePath.toLowerCase().endsWith(ExcelValidator.XLS_SIFFIX)){
			try {
				return readAsXLS(excelFilePath);
			} catch (OfficeXmlFileException e) {
				return readAsXLSX(excelFilePath);
			}
		}else{
			try {
				return readAsXLSX(excelFilePath);
			} catch (OLE2NotOfficeXmlFileException e) {
				return readAsXLS(excelFilePath);
			}
		}
	}
	
	public <T> List<T> read(Class<T> clazz){
		List<String> rows = read();
		if(rows == null || rows.size() <= 1)throw new ExcelOperBaseException("记录不存在");
		return ExcelBeanHelper.setRowValues(clazz, rows);
	} 
	
	
	public <T> void read(Class<T> clazz,ResultProcessor<T> processor){
		
	} 
	
	private List<String> readAsXLS(String path){
		try {				
			XLS2CSV xls2csv = new XLS2CSV(path, -1);
			return xls2csv.process();
		} catch (Exception e) {
			if(e instanceof org.apache.poi.poifs.filesystem.NotOLE2FileException){
				throw new ExcelOperBaseException("请选择合法的excel文件");
			}
			if(e instanceof IOException){
				throw new ExcelOperBaseException("文件读取失败");
			}
			if(e instanceof OfficeXmlFileException){
				throw (OfficeXmlFileException)e;
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
			if(e instanceof IOException){
				throw new ExcelOperBaseException(path);
			}
			if(e instanceof InvalidOperationException){
				throw new ExcelOperBaseException(e);
			}
			if(e instanceof OLE2NotOfficeXmlFileException){
				throw (OLE2NotOfficeXmlFileException)e;
			}
			throw new RuntimeException(e);
		}finally{
			try {opcPackage.close();} catch (Exception e) {}
		}
	}
	
}
