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
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.dromara.mendmix.common.excel.annotation.TitleCell;



public final class ExcelReader implements Closeable {

    private static final Logger     LOG    = LoggerFactory.getLogger(ExcelReader.class);
    /**
     * 时日类型的数据默认格式化方式
     */
    private              DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private       int      startRow;
    private       String   sheetName;
    private final String   excelFilePath;
    private final Workbook workbook;

    /**
     * 构造方法，传入需要操作的excel文件路径
     *
     * @param excelFilePath 需要操作的excel文件的路径
     * @throws IOException            IO流异常
     * @throws InvalidFormatException 非法的格式异常
     */
    public ExcelReader(String excelFilePath) throws IOException, InvalidFormatException {
        this.startRow = 0;
        this.sheetName = "Sheet1";
        this.excelFilePath = excelFilePath;
        this.workbook = createWorkbook();
    }

    /**
     * 通过数据流操作excel，仅用于读取数据
     *
     * @param inputStream excel数据流
     * @throws IOException            IO流异常
     * @throws InvalidFormatException 非法的格式异常
     */
    public ExcelReader(InputStream inputStream) throws IOException, InvalidFormatException {
        this.startRow = 0;
        this.sheetName = "Sheet1";
        this.excelFilePath = "";
        this.workbook = WorkbookFactory.create(inputStream);
    }

    /**
     * 通过数据流操作excel
     *
     * @param inputStream excel数据流
     * @param outFilePath 输出的excel文件路径
     * @throws IOException            IO流异常
     * @throws InvalidFormatException 非法的格式异常
     */
    public ExcelReader(InputStream inputStream, String outFilePath) throws IOException, InvalidFormatException {
        this.startRow = 0;
        this.sheetName = "Sheet1";
        this.excelFilePath = outFilePath;
        this.workbook = WorkbookFactory.create(inputStream);
    }

    /**
     * 开始读取的行数，这里指的是标题内容行的行数，不是数据开始的那行
     *
     * @param startRow 开始行数
     */
    public void setStartRow(int startRow) {
        if (startRow < 1) {
            throw new RuntimeException("最小为1");
        }
        this.startRow = --startRow;
    }

    /**
     * 设置需要读取的sheet名字，不设置默认的名字是Sheet1，也就是excel默认给的名字，所以如果文件没有自已修改，这个方法也就不用调了
     *
     * @param sheetName 需要读取的Sheet名字
     */
    public void setSheetName(String sheetName) {
//        Sheet sheet = this.workbook.getSheet(sheetName);
//        if (null == sheet) {
//            throw new RuntimeException("sheetName:" + sheetName + " is not exist");
//        }
        this.sheetName = sheetName;
    }

    /**
     * 设置时间数据格式
     *
     * @param format 格式
     */
    public void setFormat(String format) {
        this.format = new SimpleDateFormat(format);
    }

    /**
     * 解析读取excel文件
     *
     * @param clazz 对应的映射类型
     * @param <T>   泛型
     * @return 读取结果
     */
    public <T> List<T> parse(Class<T> clazz) {
        List<T> resultList = null;
        try {
            Sheet sheet = workbook.getSheet(this.sheetName);
            if (null != sheet) {
                resultList = new ArrayList<T>(sheet.getLastRowNum() - 1);
                Row row = sheet.getRow(this.startRow);

                Map<String, Field> fieldMap = new HashMap<String, Field>();
                Map<String, String> titleMap = new HashMap<String, String>();

                Field[] fields = clazz.getDeclaredFields();
                //这里开始处理映射类型里的注解
                for (Field field : fields) {
                    if (field.isAnnotationPresent(TitleCell.class)) {
                        TitleCell mapperCell = field.getAnnotation(TitleCell.class);
                        fieldMap.put(mapperCell.name(), field);
                    }
                }

                for (Cell title : row) {
                    CellReference cellRef = new CellReference(title);
                    titleMap.put(cellRef.getCellRefParts()[2], title.getRichStringCellValue().getString());
                }

                for (int i = this.startRow + 1; i <= sheet.getLastRowNum(); i++) {
                    T t = clazz.newInstance();
                    Row dataRow = sheet.getRow(i);
                    for (Cell data : dataRow) {
                        CellReference cellRef = new CellReference(data);
                        String cellTag = cellRef.getCellRefParts()[2];
                        String name = titleMap.get(cellTag);
                        Field field = fieldMap.get(name);
                        if (null != field) {
                            field.setAccessible(true);
                            getCellValue(data, t, field);
                        }
                    }
                    resultList.add(t);
                }
            } else {
                throw new RuntimeException("sheetName:" + this.sheetName + " is not exist");
            }
        } catch (InstantiationException e) {
            LOG.error("初始化异常", e);
        } catch (IllegalAccessException e) {
            LOG.error("初始化异常", e);
        } catch (ParseException e) {
            LOG.error("时间格式化异常:{}", e);
        } catch (Exception e) {
            LOG.error("其他异常", e);
        }
        return resultList;
    }


    private void getCellValue(Cell cell, Object o, Field field) throws IllegalAccessException, ParseException {
        LOG.debug("MENDMIX-TRACE-LOGGGING-->> cell:{}, field:{}, type:{}", cell.getCellTypeEnum(), field.getName(), field.getType().getName());
        switch (cell.getCellTypeEnum()) {
            case BLANK:
                break;
            case BOOLEAN:
                field.setBoolean(o, cell.getBooleanCellValue());
                break;
            case ERROR:
                field.setByte(o, cell.getErrorCellValue());
                break;
            case FORMULA:
                field.set(o, cell.getCellFormula());
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    if (field.getType().getName().equals(Date.class.getName())) {
                        field.set(o, cell.getDateCellValue());
                    } else {
                        field.set(o, format.format(cell.getDateCellValue()));
                    }
                } else {
                    if (field.getType().isAssignableFrom(Integer.class) || field.getType().getName().equals("int")) {
                        field.setInt(o, (int) cell.getNumericCellValue());
                    } else if (field.getType().isAssignableFrom(Short.class) || field.getType().getName().equals("short")) {
                        field.setShort(o, (short) cell.getNumericCellValue());
                    } else if (field.getType().isAssignableFrom(Float.class) || field.getType().getName().equals("float")) {
                        field.setFloat(o, (float) cell.getNumericCellValue());
                    } else if (field.getType().isAssignableFrom(Byte.class) || field.getType().getName().equals("byte")) {
                        field.setByte(o, (byte) cell.getNumericCellValue());
                    } else if (field.getType().isAssignableFrom(Double.class) || field.getType().getName().equals("double")) {
                        field.setDouble(o, cell.getNumericCellValue());
                    } else if (field.getType().isAssignableFrom(String.class)) {
                        String s = String.valueOf(cell.getNumericCellValue());
                        if (s.contains("E")) {
                            s = s.trim();
                            BigDecimal bigDecimal = new BigDecimal(s);
                            s = bigDecimal.toPlainString();
                        }
                        //防止整数判定为浮点数
                        if (s.endsWith(".0"))
                            s = s.substring(0, s.indexOf(".0"));
                        field.set(o, s);
                    } else {
                        field.set(o, cell.getNumericCellValue());
                    }
                }
                break;
            case STRING:
                if (field.getType().getName().equals(Date.class.getName())) {
                    field.set(o, format.parse(cell.getRichStringCellValue().getString()));
                } else {
                    field.set(o, cell.getRichStringCellValue().getString());
                }
                break;
            default:
                field.set(o, cell.getStringCellValue());
                break;
        }
    }

    private Workbook createWorkbook() throws IOException, InvalidFormatException {
        Workbook workbook;
        File file = new File(this.excelFilePath);
        if (!file.exists()) {
            LOG.warn("MENDMIX-TRACE-LOGGGING-->> 文件:{} 不存在！创建此文件！", this.excelFilePath);
            if (!file.createNewFile()) {
                throw new IOException("文件创建失败");
            }
            workbook = new XSSFWorkbook();
        } else {
            workbook = WorkbookFactory.create(file);
        }
        return workbook;
    }


    /**
     * 获取指定单元格的值
     *
     * @param rowNumber  行数，从1开始
     * @param cellNumber 列数，从1开始
     * @return 该单元格的值
     */
    public String getCellValue(int rowNumber, int cellNumber) {
        String result;
        checkRowAndCell(rowNumber, cellNumber);
        Sheet sheet = this.workbook.getSheet(this.sheetName);
        Row row = sheet.getRow(--rowNumber);
        Cell cell = row.getCell(--cellNumber);
        switch (cell.getCellTypeEnum()) {
            case BLANK:
                result = cell.getStringCellValue();
                break;
            case BOOLEAN:
                result = String.valueOf(cell.getBooleanCellValue());
                break;
            case ERROR:
                result = String.valueOf(cell.getErrorCellValue());
                break;
            case FORMULA:
                result = cell.getCellFormula();
                break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    result = format.format(cell.getDateCellValue());
                } else {
                    result = String.valueOf(cell.getNumericCellValue());
                }
                break;
            case STRING:
                result = cell.getRichStringCellValue().getString();
                break;
            default:
                result = cell.getStringCellValue();
                break;
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        this.workbook.close();
    }

    private void checkRowAndCell(int rowNumber, int cellNumber) {
        if (rowNumber < 1) {
            throw new RuntimeException("rowNumber less than 1");
        }
        if (cellNumber < 1) {
            throw new RuntimeException("cellNumber less than 1");
        }
    }
}
