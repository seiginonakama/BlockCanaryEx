package com.letv.sarrsdesktop.blockcanaryex

import javassist.*
import javassist.bytecode.ClassFile
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * modified from https://github.com/jasonross/NuwaGradle
 *
 * author: jixin.jia zhoulei date: 2017/3/11.
 */
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
        if (ctConstructor.isEmpty()) {
            return;
        }
        insertSamplerCode(clazz, ctConstructor)
    }

    static void insertSamplerCode(CtClass clazz, CtBehavior ctBehavior) {
        ctBehavior.addLocalVariable("__bl_stn", CtClass.longType);
        ctBehavior.addLocalVariable("__bl_stt", CtClass.longType);
        ctBehavior.addLocalVariable("__bl_icl", CtClass.booleanType);
        boolean isActivityCreating = false;
        String className = generateClassName(clazz)
        String paramTypes = generateParamTypes(getParameterTypes(ctBehavior))
        if (ctBehavior.name == "onCreate"
                && paramTypes == 'android.os.Bundle'
                && isChildOf(clazz, "android.app.Activity")) {
            isActivityCreating = true;
        }
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
                       ${
                    isActivityCreating ? "com.letv.sarrsdesktop.blockcanaryex.jrt.internal.MethodSampler.reportActivityCreated(\"${className}\");" : ""
                }
                       com.letv.sarrsdesktop.blockcanaryex.jrt.internal.MethodSampler.onMethodExit(__bl_stn, __bl_stt, "${
                    className
                }", "${ctBehavior.name}", "${paramTypes}");
                   }
                """)
    }

    static boolean isChildOf(CtClass ctClass, String className) {
        if (ctClass.name == className) {
            return true
        } else {
            try {
                CtClass superCls = ctClass.getSuperclass()
                if (superCls != null) {
                    return isChildOf(superCls, className)
                } else {
                    return false;
                }
            } catch (NotFoundException e) {
                println("warning: can't find super class in classPool! " + e.toString())
                if (ctClass.getClassFile2().getSuperclass() == className) {
                    return true;
                }
                return false;
            }
        }
    }

    static String generateClassName(CtClass clazz) {
        String clazzName = clazz.getName()
        int index$ = clazzName.lastIndexOf("\$")
        if (index$ < 0) {
            return clazzName
        } else {
            String suffix = clazzName.subSequence(index$ + 1, clazzName.length())
            if (Character.isDigit(suffix.charAt(0))) {
                //anonymous class
                String parentSimpleName;
                ClassFile classFile = clazz.getClassFile2()
                if (classFile.getSuperclass() == "java.lang.Object") {
                    String[] interfaces = classFile.getInterfaces();
                    if (interfaces != null && interfaces.length > 0) {
                        parentSimpleName = interfaces[0];
                    } else {
                        parentSimpleName = Object.class.simpleName;
                    }
                } else {
                    parentSimpleName = classFile.getSuperclass();
                }
                return clazzName + "\$" + parentSimpleName;
            } else {
                return clazzName;
            }
        }
    }

    static String generateParamTypes(String[] paramTypes) {
        if(paramTypes == null) {
            return "";
        }
        StringBuilder argTypesBuilder = new StringBuilder();
        for (int i = 0; i < paramTypes.length; i++) {
            argTypesBuilder.append(paramTypes[i])
            if (i != paramTypes.length - 1) {
                argTypesBuilder.append(",")
            }
        }
        return argTypesBuilder.toString()
    }

    static byte[] processClass(File file) {
        File optClass = new File(file.getParent(), file.name + ".opt")

        FileInputStream inputStream = new FileInputStream(file);
        FileOutputStream outputStream = new FileOutputStream(optClass)

        def bytes = injectSampler(inputStream);
        outputStream.write(bytes)
        IOUtils.closeQuietly(inputStream)
        IOUtils.closeQuietly(outputStream)
        FileUtils.forceDelete(file)
        FileUtils.moveFile(optClass, file)
        if (optClass.exists()) {
            optClass.delete()
        }
        return bytes
    }

    static boolean shouldProcessClassInJar(String entryName, Set<String> includePackage, Set<String> excludePackage, Set<String> excludeClass) {
        if (!entryName.endsWith(".class")) {
            return false;
        }
        if (entryName.contains("/R\$") || entryName.endsWith("/R.class") || entryName.endsWith("/BuildConfig.class"))
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

    /**
     * modified from javassist.bytecode.Descriptor.getParameterTypes()
     */
    public static String[] getParameterTypes(CtBehavior ctBehavior)
            throws NotFoundException {
        String desc = ctBehavior.getMethodInfo2().getDescriptor();
        if (desc.charAt(0) != '(')
            return null;
        else {
            int num = numOfParameters(desc);
            String[] args = new CtClass[num];
            int n = 0;
            int i = 1;
            while (i > 0) {
                i = toClassName(desc, i, args, n++);
            }
            return args;
        }
    }

    private static int numOfParameters(String desc) {
        int n = 0;
        int i = 1;
        for (; ;) {
            char c = desc.charAt(i);
            if (c == ')'.charAt(0))
                break;

            while (c == '['.charAt(0))
                c = desc.charAt(++i);

            if (c == 'L'.charAt(0)) {
                i = desc.indexOf(';', i) + 1;
                if (i <= 0)
                    throw new IndexOutOfBoundsException("bad descriptor");
            } else
                ++i;

            ++n;
        }

        return n;
    }

    private static int toClassName(String desc, int i,
                                   String[] args, int n)
            throws NotFoundException {
        int i2;
        String name;

        int arrayDim = 0;
        char c = desc.charAt(i);
        while (c == '['.charAt(0)) {
            ++arrayDim;
            c = desc.charAt(++i);
        }

        if (c == 'L'.charAt(0)) {
            i2 = desc.indexOf(';', ++i);
            name = desc.substring(i, i2++).replace('/', '.');
        } else {
            String type = toPrimitiveClass(c);
            if (type == null)
                return -1; // error

            i2 = i + 1;
            if (arrayDim == 0) {
                args[n] = type;
                return i2; // neither an array type or a class type
            } else
                name = type;
        }

        if (arrayDim > 0) {
            StringBuffer sbuf = new StringBuffer(name);
            while (arrayDim-- > 0)
                sbuf.append("[]");

            name = sbuf.toString();
        }

        args[n] = name;
        return i2;
    }

    static String toPrimitiveClass(char c) {
        String type = null;
        switch (c) {
            case 'Z':
                type = 'boolean';
                break;
            case 'C':
                type = 'char';
                break;
            case 'B':
                type = 'byte';
                break;
            case 'S':
                type = 'short';
                break;
            case 'I':
                type = 'int';
                break;
            case 'J':
                type = 'long';
                break;
            case 'F':
                type = 'float';
                break;
            case 'D':
                type = 'double';
                break;
            case 'V':
                type = 'void';
                break;
        }

        return type;
    }
}

