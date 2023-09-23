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
package com.mendmix.common2.sequence;

import java.util.Date;

/**
 * 
 * 
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">jiangwei</a>
 * @date 2022年6月1日
 */
public class SequenceRule {

	private String id;
	private String code;
	private String prefix;
	private String timeExpr;
	private Integer seqLength;
	private String randomType;
	private Integer randomLength;
	private Integer firstSequence = 1;
	private Integer lastSequence;
	private Date updatedAt;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getCode() {
		return code;
	}
	public void setCode(String code) {
		this.code = code;
	}
	public String getPrefix() {
		return prefix;
	}
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	public String getTimeExpr() {
		return timeExpr;
	}
	public void setTimeExpr(String timeExpr) {
		this.timeExpr = timeExpr;
	}
	public Integer getSeqLength() {
		return seqLength;
	}
	public void setSeqLength(Integer seqLength) {
		this.seqLength = seqLength;
	}
	public String getRandomType() {
		return randomType;
	}
	public void setRandomType(String randomType) {
		this.randomType = randomType;
	}
	public Integer getRandomLength() {
		return randomLength;
	}
	public void setRandomLength(Integer randomLength) {
		this.randomLength = randomLength;
	}
	public Integer getFirstSequence() {
		return firstSequence;
	}
	public void setFirstSequence(Integer firstSequence) {
		this.firstSequence = firstSequence;
	}
	public Integer getLastSequence() {
		return lastSequence;
	}
	public void setLastSequence(Integer lastSequence) {
		this.lastSequence = lastSequence;
	}
	public Date getUpdatedAt() {
		return updatedAt;
	}
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
}
