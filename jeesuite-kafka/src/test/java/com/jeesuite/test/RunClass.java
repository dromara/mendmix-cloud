/**
 * 
 */
package com.jeesuite.test;

import java.io.IOException;

import kafka.admin.ConsumerGroupCommand;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2016年12月7日
 */
public class RunClass {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		//group-1是消费者的group名称,可以在zk中
		//args = new String[]{"--zookeeper=127.0.0.1:2181","--group=kafka-demo2"};
		//ConsumerOffsetChecker.main(arr);
		args = new String[]{"--bootstrap-server=127.0.0.1:9092","--group=kafka-demo2","--new-consumer","--describe"};
		
//		File f=new File("/Users/ayg/Desktop/out.txt");
//        f.createNewFile();
//        FileOutputStream fileOutputStream = new FileOutputStream(f);
//        PrintStream printStream = new PrintStream(fileOutputStream);
//        System.setOut(printStream);
		ConsumerGroupCommand.main(args);
		
		
	}

}
