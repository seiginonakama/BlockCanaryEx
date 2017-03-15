//copy form javassist
package com.letv.sarrsdesktop.blockcanaryex;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javassist.ClassPath;
import javassist.NotFoundException;

/**
 * author: zhoulei date: 2017/3/1.
 */
final class JarClassPath implements ClassPath {
    JarFile jarfile;
    String jarfileURL;

    JarClassPath(String pathname) throws NotFoundException {
        try {
            jarfile = new JarFile(pathname);
            jarfileURL = new File(pathname).getCanonicalFile()
                    .toURI().toURL().toString();
            return;
        }
        catch (IOException e) {}
        throw new NotFoundException(pathname);
    }

    public InputStream openClassfile(String classname)
            throws NotFoundException
    {
        try {
            String jarname = classname.replace('.', '/') + ".class";
            JarEntry je = jarfile.getJarEntry(jarname);
            if (je != null)
                return jarfile.getInputStream(je);
            else
                return null;    // not found
        }
        catch (IOException e) {}
        throw new NotFoundException("broken jar file?: "
                + jarfile.getName());
    }

    public URL find(String classname) {
        String jarname = classname.replace('.', '/') + ".class";
        JarEntry je = jarfile.getJarEntry(jarname);
        if (je != null)
            try {
                return new URL("jar:" + jarfileURL + "!/" + jarname);
            }
            catch (MalformedURLException e) {}

        return null;            // not found
    }

    public void close() {
        try {
            jarfile.close();
            jarfile = null;
        }
        catch (IOException e) {}
    }

    public String toString() {
        return jarfile == null ? "<null>" : jarfile.toString();
    }
}
