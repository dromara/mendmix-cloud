package org.dromara.mendmix.mybatis.plugin.rewrite;

/**
 * 
 * <br>
 * @author jiangwei
 * @version 1.0.0
 * @date 2023年2月20日
 */
public class ConditionPair {

	private String column;
	private String[] values;
	
	public ConditionPair(String column, String[] values) {
		super();
		this.column = column;
		this.values = values;
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}

	public String[] getValues() {
		return values;
	}

	public void setValues(String[] values) {
		this.values = values;
	}
	
	
	
}
