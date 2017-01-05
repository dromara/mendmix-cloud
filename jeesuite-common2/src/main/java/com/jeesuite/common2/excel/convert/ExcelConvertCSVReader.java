package com.jeesuite.common2.excel.convert;

import java.io.IOException;
import java.util.List;

import org.apache.poi.openxml4j.exceptions.InvalidOperationException;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.poifs.filesystem.OfficeXmlFileException;

import com.jeesuite.common2.excel.ExcelOperBaseException;
import com.jeesuite.common2.excel.helper.ExcelBeanHelper;
import com.jeesuite.common2.excel.helper.ExcelValidator;

public class ExcelConvertCSVReader {

	
	public static List<String> read(String path){

		if(path.toLowerCase().endsWith(ExcelValidator.XLS_SIFFIX)){
			try {
				return readAsXLS(path);
			} catch (OfficeXmlFileException e) {
				return readAsXLSX(path);
			}
		}else{
			try {
				return readAsXLSX(path);
			} catch (OLE2NotOfficeXmlFileException e) {
				return readAsXLS(path);
			}
		}
	}
	
	public static <T> List<T> read(String path,Class<T> clazz){
		List<String> rows = read(path);
		if(rows == null || rows.size() <= 1)throw new ExcelOperBaseException("记录不存在");
		return ExcelBeanHelper.setRowValues(clazz, rows);
	} 
	
	
	private static List<String> readAsXLS(String path){
		try {				
			XLS2CSV xls2csv = new XLS2CSV(path, -1);
			return xls2csv.process();
		} catch (Exception e) {
			if(e instanceof IOException){
				throw new ExcelOperBaseException(path+"不存在");
			}
			if(e instanceof OfficeXmlFileException){
				throw (OfficeXmlFileException)e;
			}
			throw new RuntimeException(e);
		}
	}
	
	
	private static List<String> readAsXLSX(String path){
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
