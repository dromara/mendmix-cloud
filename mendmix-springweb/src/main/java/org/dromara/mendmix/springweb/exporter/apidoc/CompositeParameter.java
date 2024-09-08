package org.dromara.mendmix.springweb.exporter.apidoc;

import java.util.ArrayList;
import java.util.List;

import org.dromara.mendmix.common.constants.ValueType;
import org.dromara.mendmix.common.util.BeanUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * 
 * <br>
 * @author vakinge(vakinge)
 * @date 2023年7月27日
 */
@JsonInclude(Include.NON_NULL)
public class CompositeParameter extends SimpleParameter {


	private CompositeParameter arrayItems;
	private List<CompositeParameter> attributes;
	
	public CompositeParameter() {}
	
	public CompositeParameter(String key,ValueType type) { 
		super(key, key);
		setType(type);
	}
	
	public CompositeParameter(String key,Class<?> typeClass) {
		super(key, key);
		if(typeClass == boolean.class || typeClass == Boolean.class) {
			setType(ValueType.bool);
		}else if(typeClass == int.class 
				|| typeClass == Integer.class 
				|| typeClass == long.class
				|| typeClass == Long.class) {
			setType(ValueType.integer);
		} else if(!BeanUtils.isSimpleDataType(typeClass)){
			setType(ValueType.object);
		}else {
			setType(ValueType.string);
		}
	}

	public CompositeParameter getArrayItems() {
		return arrayItems;
	}
	public void setArrayItems(CompositeParameter arrayItems) {
		this.arrayItems = arrayItems;
	}

	public List<CompositeParameter> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<CompositeParameter> attributes) {
		this.attributes = attributes;
	}

	public void addChild(CompositeParameter parameter) {
		if(this.attributes == null)this.attributes = new ArrayList<>();
		this.attributes.add(parameter);
	}
}
