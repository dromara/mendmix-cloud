package com.jeesuite.common2.excel.helper;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;

public class ExcelValidator {
	
	public static final String XLS_SIFFIX = "xls";
	public static final String XLSX_SIFFIX = "xlsx";
	
	public static final String FIELD_SPLIT = ":";
	public final static String QUOTE = "\"";
	
	public final static String BLANK = "";

	private static Pattern blankPattern = Pattern.compile("^[,|\"]+$");
	
	public static boolean isBlankCSVRow(String line){
		if(StringUtils.isBlank(line))return true;
		return blankPattern.matcher(line).matches();
	}
	
	
	public static boolean isHSSF(String path){
		OPCPackage p = null;
		try {
			p = OPCPackage.open(path, PackageAccess.READ);
			return true;
		} catch (OLE2NotOfficeXmlFileException e) {
			return false;
		} catch (Exception e) {
			return false;
		}finally{
			try {p.close();} catch (IOException e) {}
		}
	}
}
