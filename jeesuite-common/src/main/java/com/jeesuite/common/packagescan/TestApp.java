/**
 * 
 */
package com.jeesuite.common.packagescan;

import java.util.Collection;

/**
 * @description <br>
 * @author <a href="mailto:wei.jiang@lifesense.com">vakin</a>
 * @date 2016年5月17日
 * @Copyright (c) 2015, lifesense.com
 */
public class TestApp {

	public static void main(String[] args) {
		Collection<String> cls = new PackageScanner().scanMatchPackages("com.lifesense.base.utils.packagescan");
		System.out.println(cls);
	}
}
