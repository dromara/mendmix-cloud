package com.jeesuite.common.guid;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import com.jeesuite.common.util.DateUtils;
import com.jeesuite.common.util.NodeNameHolder;

/**
 * 带时间错信息全局唯一id生成器
 * 
 * <br>
 * Class Name   : TimestampGUIDGenarator
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 15, 2021
 */
public class TimestampGUIDGenarator {

	private static final String TIME_PATTERN = "yyMMddHHmmss";
	private static String[] paddingzeros = new String[]{"","0","00","000","0000","00000","000000","0000000","00000000","000000000"};
	private static final int WORKER_ID =  NodeNameHolder.WORKER_ID;

	private final AtomicInteger NEXT_COUNTER;
	private int incrMin;
	private int incrMax;
	
	

	public TimestampGUIDGenarator(int incrMax) {
		this.incrMin = Integer.parseInt("1" + String.valueOf(incrMax).substring(1).replaceAll("[0-9]{1}", "0"));
		this.incrMax = incrMax;
		NEXT_COUNTER = new AtomicInteger(RandomUtils.nextInt(incrMin, incrMax/2));
	}

	public String next(String...prefixs) {
		StringBuilder sb = new StringBuilder();
		if(prefixs != null && prefixs.length > 0 && prefixs[0] != null) {
			sb.append(prefixs[0]);
		}
		sb.append(DateUtils.format(new Date(),TIME_PATTERN));
		sb.append(WORKER_ID);
		sb.append(buildIncrNumSequence());
		return sb.toString();
	}

	private String buildIncrNumSequence() {
		int next = NEXT_COUNTER.incrementAndGet();
		if(incrMax - next < 5){
			next = NEXT_COUNTER.updateAndGet( (x) -> x >= incrMax ? RandomUtils.nextInt(incrMin, incrMax/2) : x + 1);
		}
		String seq = String.valueOf(next);
		//补0
		int len = 5 - seq.length();
		if(len > 0){
			seq = paddingzeros[len] + seq;   
		}
		return seq;
	}
	
	public static void main(String[] args) {
		TimestampGUIDGenarator genarator = new TimestampGUIDGenarator(9999);
		
		for (int i = 0; i < 10; i++) {
			System.out.println(genarator.next());
		}
	}

}
