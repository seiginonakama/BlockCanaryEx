//copy form javassist
package com.letv.sarrsdesktop.blockcanaryex;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javassist.ClassPath;

/**
 * author: zhoulei date: 2017/3/1.
 */
final class DirClassPath implements ClassPath {
    String directory;

    DirClassPath(String dirName) {
        directory = dirName;
    }

    public InputStream openClassfile(String classname) {
        try {
            char sep = File.separatorChar;
            String filename = directory + sep
                    + classname.replace('.', sep) + ".class";
            return new FileInputStream(filename.toString());
        }
        catch (FileNotFoundException e) {}
        catch (SecurityException e) {}
        return null;
    }

    public URL find(String classname) {
        char sep = File.separatorChar;
        String filename = directory + sep
                + classname.replace('.', sep) + ".class";
        File f = new File(filename);
        if (f.exists())
            try {
                return f.getCanonicalFile().toURI().toURL();
            }
            catch (MalformedURLException e) {}
            catch (IOException e) {}

        return null;
    }

    public void close() {}

    public String toString() {
        return directory;
    }
}
