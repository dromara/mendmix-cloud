package fastdfs;

import java.io.File;

import com.jeesuite.filesystem.FileSystemClient;

public class FileSystemClientTest {

	public static void main(String[] args) {
		
		//上传到全局公有
		String url = FileSystemClient.getPublicClient().upload(new File("/Users/jiangwei/readme.txt"));
		System.out.println(url);
		
		//上传到全局私有空间
		url = FileSystemClient.getPrivateClient().upload("readme2.txt", new File("/Users/jiangwei/readme.txt"));
		//生成私有下载链接
		String downloadUrl = FileSystemClient.getPrivateClient().getDownloadUrl(url);
		
		//上传到自定义空间
		url = FileSystemClient.getClient("report").upload(new File("/Users/jiangwei/report.xls"));
	}

}
