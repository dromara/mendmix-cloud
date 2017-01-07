/**
 * 
 */
package factory;

import java.io.File;

import com.jeesuite.filesystem.FSProvider;
import com.jeesuite.filesystem.factory.FSClientFactory;

/**
 * @description <br>
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @date 2017年1月7日
 */
public class FSClientFactoryTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FSProvider provider = FSClientFactory.build("qiniu", "jw56121830");
		
		String url = provider.upload("test", null, new File("/Users/ayg/Desktop/logo.gif"));
		
		System.out.println(url);
		
	}

}
