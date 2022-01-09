package com.jeesuite.common.model;

import com.jeesuite.common.constants.DataPermPolicy;

public class DataPermItem extends KeyValues {

	private DataPermPolicy policy = DataPermPolicy.acceptOnMatch;

	public DataPermPolicy getPolicy() {
		return policy;
	}

	public void setPolicy(DataPermPolicy policy) {
		this.policy = policy;
	}
	
	
}
