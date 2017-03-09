package com.letv.sarrsdesktop.blockcanaryex

import com.letv.sarrsdesktop.blockcanaryex.DirClassPath
import com.letv.sarrsdesktop.blockcanaryex.JarClassPath
import javassist.*

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class SamplerInjecter {
    private static ClassPool classPool;

    static void setClassPath(Set<File> files) {
        classPool = new ClassPool(true);
        for (File file : files) {
            if (file.exists()) {
                if (file.isDirectory()) {
                    classPool.appendClassPath(new DirClassPath(file.absolutePath));
                } else {
                    classPool.appendClassPath(new JarClassPath(file.absolutePath));
                }
            }
        }
    }

    static processJar(File jarFile, Set<String> includePackage, Set<String> excludePackage, Set<String> excludeClass) {
        if (jarFile) {
            def optJar = new File(jarFile.getParent(), jarFile.name + ".opt")

            def file = new JarFile(jarFile);
            Enumeration enumeration = file.entries();
            JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(optJar));

            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = (JarEntry) enumeration.nextElement();
                String entryName = jarEntry.getName();
                ZipEntry zipEntry = new ZipEntry(entryName);
                InputStream inputStream = file.getInputStream(jarEntry);
                jarOutputStream.putNextEntry(zipEntry);
                if (shouldProcessClassInJar(entryName, includePackage, excludePackage, excludeClass)) {
                    def bytes = injectSampler(inputStream);
                    jarOutputStream.write(bytes);
                } else {
                    jarOutputStream.write(inputStream.getBytes());
                }
                jarOutputStream.closeEntry();
            }
            jarOutputStream.close();
            file.close();

            if (jarFile.exists()) {
                jarFile.delete()
            }
            optJar.renameTo(jarFile)
            if (optJar.exists()) {
                optJar.delete()
            }
        }

    }

    static byte[] injectSampler(InputStream inputStream) {
        CtClass clazz = classPool.makeClass(inputStream);
        if (clazz.isInterface() || clazz.isAnnotation() || clazz.isEnum() || clazz.isArray()) {
            return clazz.toBytecode();
        }
        CtMethod[] ctMethods = clazz.getDeclaredMethods();
        for (CtMethod ctMethod : ctMethods) {
            injectMethodSamplerCode(clazz, ctMethod);
        }
        CtConstructor[] ctConstructors = clazz.getConstructors()
        for (CtConstructor ctConstructor : ctConstructors) {
            injectConstructorSamplerCode(clazz, ctConstructor);
        }
        CtConstructor[] classInitializeres = clazz.getClassInitializer()
        for (CtConstructor classInitializer : classInitializeres) {
            injectConstructorSamplerCode(clazz, classInitializer)
        }
        def bytes = clazz.toBytecode()
        clazz.defrost()
        return bytes
    }

    static void injectMethodSamplerCode(CtClass clazz, CtMethod ctMethod) {
        if (ctMethod.isEmpty() || Modifier.isNative(ctMethod.getModifiers())) {
            return;
        }
        insertSamplerCode(clazz, ctMethod)
    }

    static void injectConstructorSamplerCode(CtClass clazz, CtConstructor ctConstructor) {
        if(ctConstructor.isEmpty()) {
            return;
        }
        insertSamplerCode(clazz, ctConstructor)
    }

    static void insertSamplerCode(CtClass clazz, CtBehavior ctBehavior) {
        ctBehavior.addLocalVariable("__bl_stn", CtClass.longType);
        ctBehavior.addLocalVariable("__bl_stt", CtClass.longType);
        ctBehavior.addLocalVariable("__bl_icl", CtClass.booleanType);
        ctBehavior.insertBefore(
                """
                  __bl_stn = 0L;
                  __bl_stt = 0L;
                  __bl_icl = com.letv.sarrsdesktop.blockcanaryex.jrt.internal.MethodSampler.isConcernLooper();
                  if(__bl_icl) {
                      __bl_stn = java.lang.System.nanoTime();
                      __bl_stt = android.os.SystemClock.currentThreadTimeMillis();
                  }
                """)
        ctBehavior.insertAfter(
                """
                   if(__bl_icl) {
                       com.letv.sarrsdesktop.blockcanaryex.jrt.internal.MethodSampler.onMethodExit(__bl_stn, __bl_stt, "${clazz.name}", "${ctBehavior.name}", "${generateParamTypes(ctBehavior.parameterTypes)}");
                   }
                """)
    }

    static String generateParamTypes(CtClass[] paramTypes) {
        StringBuilder argTypesBuilder = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            CtClass paramCls = paramTypes[i]
            argTypesBuilder.append(paramCls.name)
            if (i != paramTypes.length - 1) {
                argTypesBuilder.append(",")
            }
        }
        return argTypesBuilder.toString()
    }

    static byte[] processClass(File file) {
        def optClass = new File(file.getParent(), file.name + ".opt")

        FileInputStream inputStream = new FileInputStream(file);
        FileOutputStream outputStream = new FileOutputStream(optClass)

        def bytes = injectSampler(inputStream);
        outputStream.write(bytes)
        if (file.exists()) {
            file.delete()
        }
        optClass.renameTo(file)
        inputStream.close()
        outputStream.close()
        if (optClass.exists()) {
            optClass.delete()
        }
        return bytes
    }

    static boolean shouldProcessClassInJar(String entryName, Set<String> includePackage, Set<String> excludePackage, Set<String> excludeClass) {
        if (!entryName.endsWith(".class")) {
            return false;
        }
        if (entryName.contains("/R\$") || entryName.endsWith("/R.class") || entryName.endsWith("/BuildConfig.class") || entryName.startsWith("cn/jiajixin/nuwa/") || entryName.contains("android/support/"))
            return false;
        return IncludeUtils.isIncluded(entryName, includePackage) && !IncludeUtils.isExcluded(entryName, excludePackage, excludeClass)
    }

    static void processClassPath(File inputFile, Collection<String> includePackage, Collection<String> excludePackage, Collection<String> excludeClass) {
        String path = inputFile.absolutePath
        if (inputFile.isDirectory()) {
            File[] children = inputFile.listFiles()
            for (File child : children) {
                processClassPath(child, includePackage, excludePackage, excludeClass)
            }
        } else if (path.endsWith(".jar")) {
            processJar(inputFile, includePackage, excludePackage, excludeClass)
        } else if (path.endsWith(".class") && !path.contains("${File.separator}R\$") && !path.endsWith("${File.separator}R.class") && !path.endsWith("${File.separator}BuildConfig.class")) {
            if (IncludeUtils.isIncluded(path, includePackage)) {
                if (!IncludeUtils.isExcluded(path, excludePackage, excludeClass)) {
                    processClass(inputFile)
                }
            }
        }
    }
}
