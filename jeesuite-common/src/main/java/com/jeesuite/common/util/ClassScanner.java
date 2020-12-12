package com.jeesuite.common.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ClassScanner {

	private static final String SUFFIX_CLASS = ".class";

	public static List<String> scan(String packageName) {

		ClassLoader classloader = Thread.currentThread().getContextClassLoader();
		packageName = packageName.replace('.', '/');
		Enumeration<URL> urls;

		try {
			urls = classloader.getResources(packageName);
			// test for empty
			if (!urls.hasMoreElements()) {
				System.err.println("Unable to find any resources for package '" + packageName + "'");
			}
		} catch (IOException ioe) {
			System.err.println("Could not read package: " + packageName);
			return new ArrayList<>(0);
		}

		List<String> packagePathList = findInPackageWithUrls(packageName, urls);
		
		List<String> result = new ArrayList<>(packagePathList.size());
        for (String path : packagePathList) {
        	if(path.contains("$"))continue;
        	String className = path.substring(0,path.lastIndexOf(".")).replaceAll("\\/", ".");
        	result.add(className);
		}
        return result;
	}

	static List<String> findInPackageWithUrls(String packageName, Enumeration<URL> urls) {
		List<String> localClsssOrPkgs = new ArrayList<String>();
		while (urls.hasMoreElements()) {
			try {
				URL url = urls.nextElement();
				String urlPath = url.getPath();

				// it's in a JAR, grab the path to the jar
				if (urlPath.lastIndexOf('!') > 0) {
					urlPath = urlPath.substring(0, urlPath.lastIndexOf('!'));
					if (urlPath.startsWith("/")) {
						urlPath = "file:" + urlPath;
					}
				} else if (!urlPath.startsWith("file:")) {
					urlPath = "file:" + urlPath;
				}

				File file = null;
				try {
					URL fileURL = new URL(urlPath);
					// only scan elements in the classpath that are local files
					if ("file".equals(fileURL.getProtocol().toLowerCase()))
						file = new File(fileURL.toURI());
					else
						System.out.println("Skipping non file classpath element [ " + urlPath + " ]");
				} catch (URISyntaxException e) {
					// Yugh, this is necessary as the URL might not be convertible to a URI, so
					// resolve it by the file path
					file = new File(urlPath.substring("file:".length()));
				}

				if (file != null && file.isDirectory()) {
					localClsssOrPkgs.addAll(loadImplementationsInDirectory(packageName, file));
				} else if (file != null) {
					localClsssOrPkgs.addAll(loadImplementationsInJar(file));
				}
			} catch (IOException ioe) {
				System.err.println("could not read entries: " + ioe);
			}
		}
		return localClsssOrPkgs;
	}

	static List<String> loadImplementationsInDirectory(String parent, File location) {
		File[] files = location.listFiles();
		List<String> localClsssOrPkgs = new ArrayList<String>();

		for (File file : files) {
			final String packageOrClass;
			if (parent == null || parent.length() == 0) {
				packageOrClass = file.getName();
			} else {
				packageOrClass = parent + "/" + file.getName();
			}

			if (file.isDirectory()) {
				localClsssOrPkgs.addAll(loadImplementationsInDirectory(packageOrClass, file));

				// If the parent is empty, then assume the directory's jars should be searched
			} else if ("".equals(parent) && file.getName().endsWith(".jar")) {
				localClsssOrPkgs.addAll(loadImplementationsInJar(file));
			} else {
				String pkg = packageOrClass;
				if (pkg.endsWith(SUFFIX_CLASS))
					localClsssOrPkgs.add(pkg);
			}
		}
		return localClsssOrPkgs;
	}

	static List<String> loadImplementationsInJar(File file) {

		List<String> localClsssOrPkgs = new ArrayList<String>();
		JarFile jarFile = null;
		try {
			jarFile = new JarFile(file);
			for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements();) {
				JarEntry entry = e.nextElement();
				String name = entry.getName();
				if (!entry.isDirectory()) {
					if (name.endsWith(SUFFIX_CLASS)) {
						localClsssOrPkgs.add(name);
					}
				}
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return Collections.emptyList();
		} finally {
			if (jarFile != null) {
				try {
					jarFile.close();
				} catch (IOException e) {
				}
			}
		}

		return localClsssOrPkgs;
	}
	
	public static void main(String[] args) {
		List<String> list = ClassScanner.scan("com.jeesuite.common");
		System.out.println(list);
	}
}
