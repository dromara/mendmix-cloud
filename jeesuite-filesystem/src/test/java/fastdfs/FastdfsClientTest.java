package fastdfs;

import java.io.File;
import java.io.FileOutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.jeesuite.filesystem.sdk.fdfs.FastdfsClient;
import com.jeesuite.filesystem.sdk.fdfs.FileId;
import com.jeesuite.filesystem.sdk.fdfs.FileInfo;
import com.jeesuite.filesystem.sdk.fdfs.FileMetadata;


public class FastdfsClientTest {

    FastdfsClient client;

    @Before
    public void setUp() throws Exception {
        client = FastdfsClient.newBuilder()
                .connectTimeout(3000)
                .readTimeout(100)
                .maxThreads(100)
                .tracker("120.24.185.19", 22122)
                .build();

    }

    @Test
    public void testUpload() throws Exception {
        long current = System.currentTimeMillis();
        CompletableFuture<FileId> path = client.upload("group1", new File("/Users/ayg/Desktop/logo.gif"));
        System.out.println(path.get());
        System.out.println("==========");
        System.out.println(System.currentTimeMillis() - current + " ms");
    }

    @Test
    public void testUpload2() throws Exception {
    	
    	byte[] byteArray = FileUtils.readFileToByteArray(new File("/Users/ayg/Desktop/logo.gif"));
    	CompletableFuture<FileId> path = client.upload("123.gif", byteArray);
    	FileId fileId = path.get();
    	System.out.println(fileId);
    	CompletableFuture<FileMetadata> metadataGet = client.metadataGet(fileId);
		System.out.println(metadataGet.get(10, TimeUnit.SECONDS));
        System.out.println("==========");
    }

    @Test
    public void testDownload() throws Exception {

        long current = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            File file = new File("/tmp/logo2.png");
            FileId path = FileId.fromString("group1/M00/00/00/ZfvfZlbz1EyAC4FPAAAWNZ1l3ec600.png");
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                CompletableFuture<Void> promise = client.download(path, outputStream);
                promise.get();
            }
        }
        System.out.println(System.currentTimeMillis() - current + " ms");
    }

    @Test
    public void testSetMetadata() throws Exception {
        FileId path = FileId.fromString("group1/M00/00/00/ZfvfZlbz1EyAC4FPAAAWNZ1l3ec600.png");
        FileMetadata metadata = FileMetadata.newBuilder().put("test", "test1").build();
        CompletableFuture<Void> promise = client.metadataSet(path, metadata);
        promise.get();
    }

    @Test
    public void testGetMetadata() throws Exception {
        FileId path = FileId.fromString("group1/M00/00/00/ZfvfZlbz1EyAC4FPAAAWNZ1l3ec600.png");
        CompletableFuture<FileMetadata> promise = client.metadataGet(path);
        System.out.println(promise.get().values());
    }

    @Test
    public void testGetInfo() throws Exception {
        FileId path = FileId.fromString("group1/M00/00/00/ZfvfZlbz6VuAPdosAARXBcPHPhU268.log");
        CompletableFuture<FileInfo> promise = client.infoGet(path);
        System.out.println(promise.get());
    }

    @Test
    public void testDelete() throws Exception {
        FileId path = FileId.fromString("group1/M00/00/00/ZfvfZlbz7TaAeyUeAeJOH39coH0381.dmg");
        client.delete(path).get();
    }

    @After
    public void tearDown() throws Exception {
        client.close();
    }
}
