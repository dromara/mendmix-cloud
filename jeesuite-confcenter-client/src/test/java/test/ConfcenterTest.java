package test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:test-confcenter.xml")
public class ConfcenterTest implements ApplicationContextAware{

	
	@Test
	public void test(){
		
	}
	
	
	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {}

}
