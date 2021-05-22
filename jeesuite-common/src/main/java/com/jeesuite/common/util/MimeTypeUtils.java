package com.jeesuite.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.jeesuite.common.JeesuiteBaseException;

/**
 * 文件MimeType解析工具
 * 
 * <br>
 * Class Name : MimeTypeUtils
 *
 * @author <a href="mailto:vakinge@gmail.com">vakin</a>
 * @version 1.0.0
 * @date May 17, 2021
 */
public class MimeTypeUtils {

	public static MimeType getMimeType(byte[] fileBytes) {
		String fileHead = bytesToHex(Arrays.copyOf(fileBytes, 28));
		System.out.println(fileHead);
		return getMimeType(fileHead);
	}

	/** 判断文件类型 */
	public static MimeType getMimeType(File file) throws IOException {
		// 获取文件头
		String fileHead = getFileHeader(file);
		return getMimeType(fileHead);
	}

	private static MimeType getMimeType(String fileHead) {
		if (fileHead != null && fileHead.length() > 0) {
			fileHead = fileHead.toUpperCase();
			MimeType[] mimeTypes = MimeType.values();

			for (MimeType type : mimeTypes) {
				if (fileHead.startsWith(type.getHead())) {
					return type;
				}
			}
		}
		throw new JeesuiteBaseException("NOT FOUND MimeType for:" + fileHead);
	}

	/** 读取文件头 */
	private static String getFileHeader(File file) throws IOException {
		byte[] b = new byte[28];
		InputStream inputStream = null;

		try {
			inputStream = new FileInputStream(file);
			inputStream.read(b, 0, 28);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}

		return bytesToHex(b);
	}

	/** 将字节数组转换成16进制字符串 */
	private static String bytesToHex(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	public static enum MimeType {

		/** JPEG */
		JPEG("FFD8FF","image/jpeg"),

		/** PNG */
		PNG("89504E47","image/png"),

		/** GIF */
		GIF("47494638","image/gif"),
		/** Windows bitmap */
		BMP("424D","image/bmp"),
		/** Adobe photoshop */
		PSD("38425053",""),

		/** Rich Text Format */
		RTF("7B5C727466",""),

		/** XML */
		XML("3C3F786D6C",""),

		/** HTML */
		HTML("68746D6C3E",""),

		/** doc;xls;dot;ppt;xla;ppa;pps;pot;msi;sdw;db */
		OLE2("0xD0CF11E0A1B11AE1",""),

		/** Microsoft Word/Excel */
		XLS_DOC("D0CF11E0",""),

		/** Microsoft Access */
		MDB("5374616E64617264204A",""),

		/** Word Perfect */
		WPB("FF575043",""),

		/** Postscript */
		EPS_PS("252150532D41646F6265",""),

		/** Adobe Acrobat */
		PDF("255044462D312E",""),

		/** Windows Password */
		PWL("E3828596",""),

		/** ZIP Archive */
		ZIP("504B0304","multipart/x-zip"),

		/** ARAR Archive */
		RAR("52617221",""),

		/** WAVE */
		WAV("57415645","audio/wav"),

		/** AVI */
		AVI("41564920","video/avi"),

		/** Real Audio */
		RAM("2E7261FD",""),

		/** Real Media */
		RM("2E524D46",""),

		/** Quicktime */
		MOV("6D6F6F76",""),

		/** Windows Media */
		ASF("3026B2758E66CF11",""),

		/** MIDI */
		MID("4D546864","");

		private final String head;
		private final String mimeType;
		
		private MimeType(String head, String mimeType) {
			this.head = head;
			this.mimeType = mimeType;
		}

		public String getHead() {
			return head;
		}

		public String getMimeType() {
			return mimeType;
		}

	}
}
