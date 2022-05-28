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
package com.mendmix.test.sch;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.mendmix.spring.InstanceFactory;


public class TaskServerNode1 {
	
	private static Logger logger = LoggerFactory.getLogger(TaskServerNode1.class);
    public static void main(String[] args) throws InterruptedException{

        final ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("test-schduler.xml");

        InstanceFactory.setApplicationContext(context);
        
        logger.info("TASK started....");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			
			@Override
			public void run() {
			   logger.info("TASK Stoped....");
			   context.close();
			}
		}));
        
        Scanner scan=new Scanner(System.in); 
		String cmd=scan.next();
		if("q".equals(cmd)){
			scan.close();
			context.close();
		}

    }
}
