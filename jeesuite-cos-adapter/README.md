## 介绍
目前各大云厂商都提供了云对象存储服务(COS)，为了简化接入成本以及实现一套代码兼容各种云厂商，于是就有了本项目。本项目作为各云存储服务的中间适配层，只需要修改配置即可轻松切换云存储服务提供商。目前支持：七牛、阿里云、腾讯云。

##  快速使用
如果服务只存在一套文件服务配置，我们提供了一个默认客户端。只需要提供如下配置：

```
#可选：aliyun,qcloud,qiniu
jeesuite.cos.adapter.type=qiniu
jeesuite.cos.adapter.accessKey=
jeesuite.cos.adapter.xxx= 
```

用法：

```java
public void test() {
		
		CosProvider provider = CosDefaultClientBuilder.getProvider();
		
		String bucketName = "jeesuite";
		//创建bucket
		provider.createBucket(bucketName);
		
		CUploadObject uploadObject = new CUploadObject(new File("/Users/jiangwei/Desktop/1.txt")).bucketName(bucketName).folderPath("2020/01/13");
		//上传
		CUploadResult result = provider.upload(uploadObject);
		//是否存在
		boolean exists = provider.exists("jeesuite", result.getFileKey());
		//元信息
		CObjectMetadata metadata = provider.getObjectMetadata(bucketName, result.getFileKey());
		//删除
		provider.delete(null, result.getFileKey());
		
		provider.close();
	}
```

## 文档
具体使用请查看[使用文档](http://docs.jeesuite.com/docments/jeesuite-cos-adapter.html)

## 交流微信群
![微信交流群](https://jeesuite.oss-cn-guangzhou.aliyuncs.com/weixin_group_qrcode.jpg)
