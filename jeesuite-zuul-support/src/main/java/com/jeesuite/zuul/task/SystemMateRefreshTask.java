package com.jeesuite.zuul.task;

import com.jeesuite.common2.task.SubTimerTask;
import com.jeesuite.zuul.CurrentSystemHolder;

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
