/**
 * 
 */
package factory;

import java.io.File;
import java.io.IOException;

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
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		String url;
		FSProvider provider = FSClientFactory.build("qiniu", "jw56121830");
		url = provider.upload("test", null, new File("/Users/ayg/Desktop/logo.gif"));
		System.out.println(url);
		
		FSProvider provider2 = FSClientFactory.build("fastDFS", "group1");
		
        url = provider2.upload("test", null, new File("/Users/ayg/Desktop/logo.gif"));
		
		System.out.println(url);
		
		provider2.close();
	}

}
