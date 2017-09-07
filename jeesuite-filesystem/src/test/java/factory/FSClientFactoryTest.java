/**
 * 
 */
package factory;

import java.io.File;
import java.io.IOException;

import com.jeesuite.filesystem.FSProvider;
import com.jeesuite.filesystem.UploadObject;
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
		FSProvider provider = FSClientFactory.build("qiniu", "testa1b2c3");
		url = provider.upload(new UploadObject(new File("/Users/jiangwei/Desktop/homepage.txt")));
		System.out.println(provider.getDownloadUrl(url));
//		
//		FSProvider provider2 = FSClientFactory.build("fastDFS", "group1");
//		
//        url = provider2.upload("test", null, new File("/Users/jiangwei/Desktop/logo.gif"));
//		
//		System.out.println(url);
//		
//		provider2.close();
	}

}
