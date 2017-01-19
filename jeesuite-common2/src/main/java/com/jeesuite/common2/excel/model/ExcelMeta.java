/**
 * 
 */
package com.jeesuite.common2.excel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月19日
 */
public class ExcelMeta {

	private Class<?> beanClass;
	private int titleRowNum;
	private List<TitleMeta> titleCells;
	private String[] titles;
	private TitleMeta[][] titleMetaArray;

	public ExcelMeta(Class<?> beanClass, List<TitleMeta> titleCells, int titleRowNum) {
		super();
		this.beanClass = beanClass;
		this.titleCells = titleCells;
		this.titleRowNum = titleRowNum;

		// 排序
		Collections.sort(titleCells, new Comparator<TitleMeta>() {
			@Override
			public int compare(TitleMeta o1, TitleMeta o2) {
				return o1.getColumnIndex() - o2.getColumnIndex();
			}
		});

		List<String> titleList = new ArrayList<>();
		for (int i = 0; i < titleCells.size(); i++) {
			TitleMeta tit = titleCells.get(i);
			if (tit.getChildren().isEmpty()) {
				titleList.add(tit.getTitle());
			} else {
				List<TitleMeta> children = tit.getChildren();
				for (TitleMeta titleCell : children) {
					titleList.add(titleCell.getTitle());
				}
			}

		}
		this.titles = titleList.toArray(new String[0]);
		this.titleMetaArray = new TitleMeta[titleRowNum][getTitleColumnNum()];
		
		for (int i = 0; i < titleCells.size(); i++) {
			TitleMeta tit = titleCells.get(i);			
			titleMetaArray[tit.getRowIndex() - 1][tit.getColumnIndex() - 1] = tit;
			List<TitleMeta> children = tit.getChildren();
			for (TitleMeta titleCell : children) {
				titleMetaArray[titleCell.getRowIndex() - 1][titleCell.getColumnIndex() - 1] = titleCell;
				//为了便于后面合并处理，设置父级单元格
				titleMetaArray[titleCell.getRowIndex() - 2][titleCell.getColumnIndex() - 1] = tit;
			}
		}
		
		//TODO 临时处理行为为空的，暂时这样处理，仅兼容只有两行表头的情况
		for (int i = 0; i < titleRowNum; i++) {
			for (int j = 0; j < getTitleColumnNum(); j++) {
				TitleMeta titleMeta = titleMetaArray[i][j];
				if(titleMeta == null){
					titleMetaArray[i][j] = titleMetaArray[i - 1][j];
				}
			}
		}
	}

	public Class<?> getBeanClass() {
		return beanClass;
	}

	public int getTitleRowNum() {
		return titleRowNum;
	}

	public List<TitleMeta> getTitleCells() {
		return titleCells;
	}

	public String[] getFinalTitles() {
		return titles;
	}

	public int getTitleColumnNum() {
		return titles.length;
	}
	
	public TitleMeta getTitleMeta(int row,int column){
		TitleMeta meta = titleMetaArray[row - 1][column - 1];
		return meta;
	}
}
