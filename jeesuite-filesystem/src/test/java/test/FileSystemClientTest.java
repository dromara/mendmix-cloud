package test;

import java.io.File;
import java.util.Map;

import com.jeesuite.filesystem.FileSystemClient;
import com.jeesuite.filesystem.UploadTokenParam;

public class FileSystemClientTest {

	public static void main(String[] args) {
		UploadTokenParam param = new UploadTokenParam();
		Map<String, Object> token = FileSystemClient.getClient("xxxx2").createUploadToken(param);
		
		System.out.println(token);
	}

}
