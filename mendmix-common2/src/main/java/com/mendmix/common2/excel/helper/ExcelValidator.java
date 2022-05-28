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
package com.mendmix.common2.excel.helper;

import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.OLE2NotOfficeXmlFileException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackageAccess;

public class ExcelValidator {
	
	public static final String XLS_SIFFIX = "xls";
	public static final String XLSX_SIFFIX = "xlsx";
	
	public static final String FIELD_SPLIT = "-@-";
	
	public final static String BLANK = "";
	
	public final static String SHEET_NAME_PREFIX = "{sheet}:";

	private static Pattern blankPattern = Pattern.compile("^["+FIELD_SPLIT+"|\"]+$");
	
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
