package com.jeesuite.common.packagescan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Scans the classpath for packages and tries to determine their versions
 */
public class PackageScanner {
    private Patterns packagePatterns;
    private Patterns jarPatterns;
    private ClassLoader classLoader;
    private PatternFactory patternFactory;

    public Collection<String> scanMatchPackages(String ...packagePatterns){
    	Patterns ppatterns = new Patterns(packagePatterns, new String[]{});
    	String[] clonePatterns = packagePatterns.clone();
    	for (int i = 0; i < clonePatterns.length; i++) {
    		clonePatterns[i] = clonePatterns[i].replace(".", "/");
		}
    	return this.selectPackages(ppatterns)//
    	           .scan();//
    }

    /**
     * Constructor
     */
    public PackageScanner() {
        packagePatterns = new Patterns(new String[]{"com.*","net.*","org.*"}, new String[]{});
        jarPatterns = new Patterns(new String[]{"*.jar"}, new String[]{});
        patternFactory = new SimpleWildcardPatternFactory();
    }


    public PackageScanner selectJars(Patterns jars) {
        this.jarPatterns = jars;
        return this;
    }
    
    public PackageScanner selectPackages(Patterns packages) {
        this.packagePatterns = packages;
        return this;
    }
    

    /**
     * Scans the classloader as configured.
     * @return A list of discovered packages and their guessed version
     */
    public Set<String> scan() {
        // Initialize the pattern factories
        initPatterns();

        // Determine which packages to start from
        List<String> roots = packagePatterns.getRoots();
        InternalScanner scanner = new InternalScanner(getClassLoader());

        // Kick off the scanning
        Set<String> exports = scanner.findInPackages(new PatternTest(), roots.toArray(new String[roots.size()]));

        return exports;
    }


    private void initPatterns()
    {
        this.jarPatterns.setPatternFactory(patternFactory);
        this.packagePatterns.setPatternFactory(patternFactory);
    }

    // DSL methods and classes
    /**
     * Sets the classloader to scan
     * @param classLoader The classloader
     */
    public PackageScanner useClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }


    /**
     * Sets the pattern factory to use
     * @param factory The pattern factory
     */
    public PackageScanner usePatternFactory(PatternFactory factory) {
        this.patternFactory = factory;
        return this;
    }


    /**
     * Sets what patterns to include
     * @param includes The included patterns
     */
    public static String[] include(String... includes) {
        return includes;
    }

    /**
     * Sets what patterns to exclude
     * @param includes The excluded patterns
     */
    public static String[] exclude(String... includes) {
        return includes;
    }

    /**
     * Sets the jar patterns to scan
     * @param includes The patterns to include
     * @param excludes The patterns to exclude
     */
    public static Patterns jars(String[] includes, String[] excludes) {
        return new Patterns(includes, excludes);
    }

    /**
     * Sets the jar patterns to scan
     * @param includes The patterns to include
     */
    public static Patterns jars(String[] includes) {
        return new Patterns(includes, new String[]{});
    }

    /**
     * Sets the package patterns to scan
     * @param includes The patterns to include
     * @param excludes The patterns to exclude
     */
    public static Patterns packages(String[] includes, String[] excludes) {
        return new Patterns(includes, excludes);
    }

    /**
     * Sets the package patterns to scan
     * @param includes The patterns to include
     */
    public static Patterns packages(String[] includes) {
    	return new Patterns(includes, new String[]{});
    }

    ClassLoader getClassLoader() {
        return classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
    }



    /**
     * The patterns to include and exclude
     */
    public static class Patterns {

        private String[] origIncludes;
        private String[] origExcludes;

        private List<CompiledPattern> includes;
        private List<CompiledPattern> excludes;
        private PatternFactory factory;

        /**
         * Constructs a set of patterns
         * @param includes The patterns to include
         * @param excludes The patterns to exclude
         */
        public Patterns(String[] includes, String[] excludes) {
            this.origIncludes = includes;
            this.origExcludes = excludes;
        }

        void setPatternFactory(PatternFactory factory) {
            this.factory = factory;
        }

        boolean match(String val) {
            if (includes == null) {
                compilePatterns();
            }
            for (CompiledPattern ptn : includes) {
                if (ptn.matches(val)) {
                    for (CompiledPattern exptn : excludes) {
                        if (exptn.matches(val)) {
                            return false;
                        }
                    }
                    return true;
                }
            }
            return false;
        }

        List<String> getRoots() {
            List<String> roots = new ArrayList<String>();
            for (String inc : origIncludes) {
                String root = inc;
                int starPos = root.indexOf("*");
                if (starPos > -1) {
                    int dotPos = root.lastIndexOf(".", starPos);
                    if (dotPos > -1) {
                        root = root.substring(0, dotPos);
                    }
                }
                roots.add(root);
            }
            return roots;
        }

        private void compilePatterns() {
            this.includes = new ArrayList<CompiledPattern>();
            for (String ptn : origIncludes) {
                this.includes.add(factory.compile(ptn));
            }

            this.excludes = new ArrayList<CompiledPattern>();
            for (String ptn : origExcludes) {
                this.excludes.add(factory.compile(ptn));
            }
        }
    }

    private class PatternTest implements InternalScanner.Test {
        public boolean matchesPackage(String pkg) {
            return packagePatterns.match(pkg);
        }

        public boolean matchesJar(String name) {
            return jarPatterns.match(name);
        }
    }
}
