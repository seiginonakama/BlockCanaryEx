package com.letv.sarrsdesktop.blockcanaryex

import org.apache.tools.ant.taskdefs.condition.Os

/**
 * modified from https://github.com/jasonross/NuwaGradle
 *
 * author: jixin.jia zhoulei date: 2017/3/11.
 */
class IncludeUtils {
    public static Set<String> fomartPath(Collection<String> paths) {
        Set<String> theNew = new HashSet<>()
        for(String path : paths) {
            theNew.add(path.replaceAll("\\.", "/"))
        }
        return theNew
    }

    public static boolean isExcluded(String path, Collection<String> excludePackage, Collection<String> excludeClass) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            path = path.replaceAll("\\\\", "/");
        }

        for (String exclude : excludeClass) {
            if (path.equals(exclude + ".class")) {
                return true;
            }
        }
        for (String exclude : excludePackage) {
            if (path.startsWith(exclude)) {
                return true;
            }
        }

        return false;
    }

    public static boolean isIncluded(String path, Collection<String> includePackage) {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            path = path.replaceAll("\\\\", "/");
        }

        if (includePackage.size() == 0) {
            return true
        }

        for (String include : includePackage) {
            if (path.startsWith(include)) {
                return true;
            }
        }

        return false;
    }
}
