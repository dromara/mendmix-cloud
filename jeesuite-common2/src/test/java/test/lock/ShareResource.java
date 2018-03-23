package test.lock;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class ShareResource {

	public static int add(){
		File file = new File("/Users/jiangwei/Desktop/sign.txt");
		try {
			String content = FileUtils.readFileToString(file,"UTF-8");
			int next = Integer.parseInt(StringUtils.isBlank(content) ? "0" : content) + 1;
			FileUtils.write(file, String.valueOf(next),"UTF-8", false);
			return next;
		} catch (Exception e) {
			e.printStackTrace();
			return 0;
		}
	}
}
