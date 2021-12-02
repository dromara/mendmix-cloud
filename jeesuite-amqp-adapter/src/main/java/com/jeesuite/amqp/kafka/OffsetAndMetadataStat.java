package com.jeesuite.amqp.kafka;

public class OffsetAndMetadataStat {

	private long offset;
	private boolean commited = true;

	public OffsetAndMetadataStat(long offset) {
		this.offset = offset;
	}

	public long getOffset() {
		return offset;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public boolean isCommited() {
		return commited;
	}

	public void setCommited(boolean commited) {
		this.commited = commited;
	}
	
	
	
	public void updateOnConsumed(long offset) {
		this.offset = offset;
		this.commited = false;
	}
}
