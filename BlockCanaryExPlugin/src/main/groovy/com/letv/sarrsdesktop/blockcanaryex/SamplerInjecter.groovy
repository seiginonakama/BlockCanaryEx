/*
 * Copyright (C) 2017 lqcandqq13 (https://github.com/lqcandqq13).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.letv.sarrsdesktop.blockcanaryex

import javassist.*
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils

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
        boolean isActivityCreating = false;
        String className = generateClassName(clazz)
        String paramTypes = generateParamTypes(ctBehavior.parameterTypes)
        if(isChildOf(clazz, "android.app.Activity")
                && ctBehavior.name == "onCreate"
                && paramTypes == 'android.os.Bundle') {
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
                       ${isActivityCreating ? "com.letv.sarrsdesktop.blockcanaryex.jrt.internal.MethodSampler.reportActivityCreated(\"${className}\");" : ""}
                       com.letv.sarrsdesktop.blockcanaryex.jrt.internal.MethodSampler.onMethodExit(__bl_stn, __bl_stt, "${className}", "${ctBehavior.name}", "${paramTypes}");
                   }
                """)
    }

    static boolean isChildOf(CtClass ctClass, String className) {
        if(ctClass.name == className) {
            return true
        } else {
            CtClass superCls = ctClass.getSuperclass()
            if(superCls != null) {
                return isChildOf(superCls, className)
            } else {
                return false;
            }
        }
    }

    static String generateClassName(CtClass clazz) {
        String clazzName = clazz.getName()
        int index$ = clazzName.lastIndexOf("\$")
        if(index$ < 0) {
            return clazzName
        } else {
            String suffix = clazzName.subSequence(index$ + 1, clazzName.length())
            if(Character.isDigit(suffix.charAt(0))) {
                String parentSimpleName;
                if(clazz.getSuperclass().name == "java.lang.Object") {
                    CtClass[] ctClasses = clazz.getInterfaces();
                    if(ctClasses != null && ctClasses.length > 0) {
                        parentSimpleName = ctClasses[0].simpleName;
                    } else {
                        parentSimpleName = Object.class.simpleName;
                    }
                } else {
                    parentSimpleName = clazz.getSuperclass().simpleName;
                }
                return clazzName + "\$" + parentSimpleName;
            } else {
                return clazzName;
            }
        }
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
}

