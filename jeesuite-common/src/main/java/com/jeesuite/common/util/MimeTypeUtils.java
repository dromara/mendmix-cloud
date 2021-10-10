package com.jeesuite.common.util;

import java.util.ArrayList;
import java.util.List;

import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicMatch;

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

	private static List<FileMeta> fileMetas = new ArrayList<>();

	static {
		fileMetas.add(new FileMeta("image/jpeg", "jpg"));
		fileMetas.add(new FileMeta("image/gif", "gif"));
		fileMetas.add(new FileMeta("image/png", "png"));
		fileMetas.add(new FileMeta("image/bmp", "bmp"));
		fileMetas.add(new FileMeta("text/plain", "txt"));
		fileMetas.add(new FileMeta("application/zip", "zip"));
		fileMetas.add(new FileMeta("application/x-zip-compressed", "zip"));
		fileMetas.add(new FileMeta("multipart/x-zip", "zip"));
		fileMetas.add(new FileMeta("application/x-compressed", "zip"));
		fileMetas.add(new FileMeta("audio/mpeg3", "mp3"));
		fileMetas.add(new FileMeta("video/avi", "avi"));
		fileMetas.add(new FileMeta("audio/wav", "wav"));
		fileMetas.add(new FileMeta("application/x-gzip", "gzip"));
		fileMetas.add(new FileMeta("application/x-gzip", "gz"));
		fileMetas.add(new FileMeta("text/html", "html"));
		fileMetas.add(new FileMeta("application/x-shockwave-flash", "svg"));
		fileMetas.add(new FileMeta("application/pdf", "pdf"));
		fileMetas.add(new FileMeta("application/msword", "doc"));
		fileMetas.add(new FileMeta("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"));
		fileMetas.add(new FileMeta("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx"));
		fileMetas.add(new FileMeta("application/vnd.ms-excel", "xls"));
		fileMetas.add(new FileMeta("application/vnd.ms-powerpoint", "ppt"));
		fileMetas
				.add(new FileMeta("application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx"));
	}

	public static String getFileExtension(String mimeType) {
		FileMeta meta = fileMetas.stream().filter(o -> o.mimeType.equals(mimeType)).findFirst().orElse(null);
		return meta == null ? null : meta.extension;
	}

	public static String getFileMimeType(String extension) {
		FileMeta meta = fileMetas.stream().filter(o -> o.extension.equals(extension)).findFirst().orElse(null);
		return meta == null ? null : meta.mimeType;
	}

	public static FileMeta getFileMeta(byte[] data) {
		try {
			MagicMatch magicMatch = Magic.getMagicMatch(data);
			return new FileMeta(magicMatch.getMimeType(), magicMatch.getExtension());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static class FileMeta {
		String mimeType;
		String extension;

		public FileMeta(String mimeType, String extension) {
			super();
			this.mimeType = mimeType;
			this.extension = extension;
		}

		public String getMimeType() {
			return mimeType;
		}

		public String getExtension() {
			return extension;
		}

	}

}
