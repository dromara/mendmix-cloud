package com.jeesuite.common.packagescan;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Does the actual work of scanning the classloader
 */
class InternalScanner {
    /**
	 * 
	 */
	private static final String SUFFIX_CLASS = ".class";
	private final Logger log = LoggerFactory.getLogger(InternalScanner.class);
    private Map<String,Set<String>> jarContentCache = new HashMap<String,Set<String>>();
    private ClassLoader classloader;

    static interface Test {
        boolean matchesPackage(String pkg);

        boolean matchesJar(String name);
    }

    InternalScanner(ClassLoader cl) {
        this.classloader = cl;
    }


    Set<String> findInPackages(Test test, String... roots) {
    	Set<String> localClsssOrPkgs = new HashSet<String>();
        for (String pkg : roots) {
        	localClsssOrPkgs.addAll(findInPackage(test, pkg));
        }
        return localClsssOrPkgs;
    }


    /**
     * Scans for classes starting at the package provided and descending into subpackages.
     * Each class is offered up to the Test as it is discovered, and if the Test returns
     * true the class is retained.
     *
     * @param test        an instance of {@link Test} that will be used to filter classes
     * @param packageName the name of the package from which to start scanning for
     *                    classes, e.g. {@code net.sourceforge.stripes}
     * @return List of packages to export.
     */
    List<String> findInPackage(Test test, String packageName) {
    	List<String> localClsssOrPkgs = new ArrayList<String>();

        packageName = packageName.replace('.', '/');
        Enumeration<URL> urls;

        try {
            urls = classloader.getResources(packageName);
            // test for empty
            if (!urls.hasMoreElements())
            {
                log.warn("Unable to find any resources for package '" + packageName + "'");
            }
        }
        catch (IOException ioe) {
            log.warn("Could not read package: " + packageName);
            return localClsssOrPkgs;
        }

        return findInPackageWithUrls(test, packageName, urls);
    }

    List<String> findInPackageWithUrls(Test test, String packageName, Enumeration<URL> urls)
    {
    	List<String> localClsssOrPkgs = new ArrayList<String>();
        while (urls.hasMoreElements()) {
            try {
                URL url = urls.nextElement();
                String urlPath = url.getPath();

                // it's in a JAR, grab the path to the jar
                if (urlPath.lastIndexOf('!') > 0) {
                    urlPath = urlPath.substring(0, urlPath.lastIndexOf('!'));
                    if (urlPath.startsWith("/"))
                    {
                        urlPath = "file:" + urlPath;
                    }
                } else if (!urlPath.startsWith("file:")) {
                    urlPath = "file:"+urlPath;
                }

                log.debug("Scanning for packages in [" + urlPath + "].");
                File file = null;
                try
                {
                    URL fileURL = new URL(urlPath);
                    // only scan elements in the classpath that are local files
                    if("file".equals(fileURL.getProtocol().toLowerCase()))
                        file = new File(fileURL.toURI());
                    else
                        log.info("Skipping non file classpath element [ "+urlPath+ " ]");
                }
                catch (URISyntaxException e)
                {
                    //Yugh, this is necessary as the URL might not be convertible to a URI, so resolve it by the file path
                    file = new File(urlPath.substring("file:".length()));
                }

                if (file!=null && file.isDirectory()) {
                	localClsssOrPkgs.addAll(loadImplementationsInDirectory(test, packageName, file));
                } else if (file!=null) {
                    if (test.matchesJar(file.getName())) {
                    	localClsssOrPkgs.addAll(loadImplementationsInJar(test, file));
                    }
                }
            }
            catch (IOException ioe) {
                log.error("could not read entries: " + ioe);
            }
        }
        return localClsssOrPkgs;
    }


    /**
     * Finds matches in a physical directory on a filesystem.  Examines all
     * files within a directory - if the File object is not a directory, and ends with <i>.class</i>
     * the file is loaded and tested to see if it is acceptable according to the Test.  Operates
     * recursively to find classes within a folder structure matching the package structure.
     *
     * @param test     a Test used to filter the classes that are discovered
     * @param parent   the package name up to this directory in the package hierarchy.  E.g. if
     *                 /classes is in the classpath and we wish to examine files in /classes/org/apache then
     *                 the values of <i>parent</i> would be <i>org/apache</i>
     * @param location a File object representing a directory
     * @return List of packages to export.
     */
    List<String> loadImplementationsInDirectory(Test test, String parent, File location) {
        log.debug("Scanning directory " + location.getAbsolutePath() + " parent: '" + parent + "'.");
        File[] files = location.listFiles();
        List<String> localClsssOrPkgs = new ArrayList<String>();

        for (File file : files) {
            final String packageOrClass;
            if (parent == null || parent.length() == 0)
            {
                packageOrClass = file.getName();
            }
            else
            {
                packageOrClass = parent + "/" + file.getName();
            }

            if (file.isDirectory()) {
            	localClsssOrPkgs.addAll(loadImplementationsInDirectory(test, packageOrClass, file));

            // If the parent is empty, then assume the directory's jars should be searched
            } else if ("".equals(parent) && file.getName().endsWith(".jar") && test.matchesJar(file.getName())) {
            	localClsssOrPkgs.addAll(loadImplementationsInJar(test, file));
            } else {
                String pkg = packageOrClass;
                if(pkg.endsWith(SUFFIX_CLASS))localClsssOrPkgs.add(pkg);
            }
        }
        return localClsssOrPkgs;
    }

    /**
     * Finds matching classes within a jar files that contains a folder structure
     * matching the package structure.  If the File is not a JarFile or does not exist a warning
     * will be logged, but no error will be raised.
     *
     * @param test    a Test used to filter the classes that are discovered
     * @param file the jar file to be examined for classes
     * @return List of packages to export.
     */
    List<String> loadImplementationsInJar(Test test, File file) {

        List<String> localClsssOrPkgs = new ArrayList<String>();
        Set<String> packages = jarContentCache.get(file.getPath());
        if (packages == null)
        {
            packages = new HashSet<String>();
            try {
                JarFile jarFile = new JarFile(file);


                for (Enumeration<JarEntry> e = jarFile.entries(); e.hasMoreElements(); ) {
                    JarEntry entry = e.nextElement();
                    String name = entry.getName();
                    if (!entry.isDirectory()) {
                    	if(name.endsWith(SUFFIX_CLASS)){
                    		localClsssOrPkgs.add(name);
                    	}
                     }
                }
            }
            catch (IOException ioe) {
                log.error("Could not search jar file '" + file + "' for classes matching criteria: " +
                        test + " due to an IOException" + ioe);
                return Collections.emptyList();
            }
            finally
            {
                jarContentCache.put(file.getPath(), packages);
            }
        }

        return localClsssOrPkgs;
    }

   
}
