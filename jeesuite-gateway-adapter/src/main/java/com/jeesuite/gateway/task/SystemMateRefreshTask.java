package com.jeesuite.gateway.task;

import com.jeesuite.common2.task.SubTimerTask;
import com.jeesuite.gateway.CurrentSystemHolder;

public class SystemMateRefreshTask implements SubTimerTask{

	@Override
	public void doSchedule() {
		CurrentSystemHolder.load();
	}

	@Override
	public int periodMillis() {
		return 60 * 1000;
	}

}
