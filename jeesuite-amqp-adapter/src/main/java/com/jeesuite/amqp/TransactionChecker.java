package com.jeesuite.amqp;

/**
 * 
 * <br>
 * Class Name   : TransactionChecker
 *
 * @author jiangwei
 * @version 1.0.0
 * @date 2020年8月11日
 */
public interface TransactionChecker {

	public static final String TRANSACTION_PARAM_NAME = "transactionId";
	MessageStatus check(String transactionId);
}
