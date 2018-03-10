package com.jeesuite.spring;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

/**
 * 实例工厂类。通过它可以获得其管理的类的实例。 InstanceFactory向客户代码隐藏了IoC工厂的具体实现。在后台，它通过
 * InstanceProvider策略接口，允许选择不同的IoC工厂，例如Spring， Google Guice和TapestryIoC等等。
 * IoC工厂应该在应用程序启动时装配好，也就是把初始化好的InstanceProvider
 * 实现类提供给InstanceFactory。对于web应用来说，最佳的初始化方式是创
 * 建一个Servlet过滤器或监听器，并部署到web.xml里面；对普通java应用程
 * 序来说，最佳的初始化位置是在main()函数里面；对于单元测试，最佳的初始 化位置是setUp()方法内部。
 */
public class InstanceFactory {

	private static SpringInstanceProvider instanceProvider;
	private static Long timeStarting = System.currentTimeMillis();
	private static AtomicBoolean initialized = new AtomicBoolean(false);
	private static AtomicBoolean loadFinished = new AtomicBoolean(false);

	/**
	 * 设置实例提供者。
	 * @param provider 一个实例提供者的实例。
	 */
	public static void setInstanceProvider(SpringInstanceProvider provider) {
		if(instanceProvider != null)return;
		instanceProvider = provider;
		initialized.set(true);
	}
	
	public static void loadFinished(SpringInstanceProvider provider){
		setInstanceProvider(provider);
		loadFinished.set(true);
	}

	public static boolean isLoadfinished(){
		return loadFinished.get();
	}
	
	/**
	 * 获取指定类型的对象实例。如果IoC容器没配置好或者IoC容器中找不到该类型的实例则抛出异常。
	 * 
	 * @param <T> 对象的类型
	 * @param beanClass 对象的类
	 * @return 类型为T的对象实例
	 */
	public static <T> T getInstance(Class<T> beanClass) {
		return (T) getInstanceProvider().getInstance(beanClass);
	}

	/**
	 * 获取指定类型的对象实例。如果IoC容器没配置好或者IoC容器中找不到该实例则抛出异常。
	 * 
	 * @param <T> 对象的类型
	 * @param beanName 实现类在容器中配置的名字
	 * @param beanClass 对象的类
	 * @return 类型为T的对象实例
	 */
	public static <T> T getInstance(Class<T> beanClass, String beanName) {
		return (T) getInstanceProvider().getInstance(beanClass, beanName);
	}

	/**
	 * 获取指定类型的对象实例
	 * @param <T> 对象的类型
	 * @param beanName 实现类在容器中配置的名字
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getInstance(String beanName) {
		return (T) getInstanceProvider().getInstance(beanName);
	}

	/**
	 * 获取实例提供者。
	 * @return 实体提供者的一个实现类。
	 */
	public static SpringInstanceProvider getInstanceProvider() {
		return instanceProvider;
	}
	
	public static ApplicationContext getContext(){
		return getInstanceProvider().getApplicationContext();
	}
	
	/**
	 * 这是一个阻塞方法，直到context初始化完成
	 */
	public synchronized static void waitUtilInitialized(){
		if(initialized.get())return;
		while(true){
			if(initialized.get())break;
			try {Thread.sleep(1000);} catch (Exception e) {}
			long waiting = System.currentTimeMillis() - timeStarting;
			if(waiting >60 * 1000)throw new RuntimeException("Spring Initialize failture");
			System.out.println("Spring Initializing >>>>>"+waiting + " s");
		}
	}

	public static boolean isInitialized() {
		return initialized.get();
	}
	
	

}
