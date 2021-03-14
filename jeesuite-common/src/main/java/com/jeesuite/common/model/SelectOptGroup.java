/*
 * Copyright 2016-2018 www.jeesuite.com.
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
package com.jeesuite.common.model;

import java.util.ArrayList;
import java.util.List;

import com.jeesuite.common.model.SelectOption;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2018年11月19日
 */
public class SelectOptGroup {

	private String label;
	private String value;
	private List<SelectOption> options = new ArrayList<>();
	
	public SelectOptGroup() {}
	
	public SelectOptGroup(String label) {
		this.label = label;
	}
	
	public SelectOptGroup(String label, String value) {
		this.label = label;
		this.value = value;
	}

	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public List<SelectOption> getOptions() {
		return options;
	}
	public void setOptions(List<SelectOption> options) {
		this.options = options;
	}
	
	
}
