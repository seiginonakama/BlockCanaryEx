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

import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.model.AndroidLibrary
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.hash.HashUtil

/**
 * author: zhoulei date: 2017/2/28.
 */
public class BlockCanaryExPlugin implements Plugin<Project> {
    private static final String TRANSFORM_NAME = "blockCanaryEx";

    private static final String I = File.separator;

    private static final Set<QualifiedContent.ContentType> TYPES = new HashSet<>();
    private static final Set<QualifiedContent.Scope> SCOPES = new HashSet<>();
    private final Set<QualifiedContent.Scope> mCareScopes = new HashSet<>();
    static {
        TYPES.add(QualifiedContent.DefaultContentType.CLASSES);

        SCOPES.add(QualifiedContent.Scope.PROJECT);
        SCOPES.add(QualifiedContent.Scope.PROJECT_LOCAL_DEPS);
        SCOPES.add(QualifiedContent.Scope.SUB_PROJECTS);
        SCOPES.add(QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS);
        SCOPES.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES);
    }

    private List<String> mExcludePackages = ["com.letv.sarrsdesktop.blockcanaryex.jrt.internal", "javassist"];

    private List<String> mExcludeClasses = ["com.letv.sarrsdesktop.blockcanaryex.jrt.BlockInfo", "com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx",
                                            "com.letv.sarrsdesktop.blockcanaryex.jrt.Config", "com.letv.sarrsdesktop.blockcanaryex.jrt.FrequentMethodInfo",
                                            "com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo"];

    private List<String> mIncludePackages = [];

    @Override
    void apply(Project project) {
        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)

        if (!hasApp && !hasLib) {
            throw new IllegalStateException("'android' or 'android-library' plugin required.")
        }

        if (hasLib) {
            //Transforms with scopes '[SUB_PROJECTS, SUB_PROJECTS_LOCAL_DEPS, EXTERNAL_LIBRARIES]' cannot be applied to library projects.
            SCOPES.remove(QualifiedContent.Scope.SUB_PROJECTS)
            SCOPES.remove(QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS)
            SCOPES.remove(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
        }

        project.extensions.create("block", BlockCanaryExExtension)

        BlockCanaryExExtension block = project.block;
        project.android.registerTransform(new Transform() {
            @Override
            String getName() {
                return TRANSFORM_NAME;
            }

            @Override
            Set<QualifiedContent.ContentType> getInputTypes() {
                return TYPES;
            }

            @Override
            Set<QualifiedContent.Scope> getScopes() {
                return SCOPES;
            }

            @Override
            boolean isIncremental() {
                return false;
            }

            @Override
            public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
                boolean enable = true;
                if(hasLib && !block.releaseEnabled) {
                    //library only has release status
                    println("BlockCanaryEx only enable when library project releaseEnabled = true")
                    enable = false;
                } else {
                    boolean isDebug = isDebug(transformInvocation)
                    if ((isDebug && !block.debugEnabled)
                            || (!isDebug && !block.releaseEnabled)) {
                        enable = false;
                        println("block canary ex disabled")
                    }
                }

                if(!enable) {
                    mCareScopes.clear()
                } else {
                    setFilter(block)
                    setCareScope(block.getScope())
                }

                cleanTransformsDir(project)

                Collection<TransformInput> transformInputs = transformInvocation.getInputs();
                List<File> processFileList = new ArrayList<>();
                Set<File> classPath = new HashSet<>()

                obtainProjectClassPath(project, classPath)

                BaseVariant baseVariant;
                if (hasApp) {
                    baseVariant = project.android.applicationVariants.getAt(0)
                } else {
                    baseVariant = project.android.getLibraryVariants().getAt(0)
                }

                for (TransformInput transformInput : transformInputs) {
                    Collection<JarInput> jarInputs = transformInput.getJarInputs();
                    for (JarInput jarInput : jarInputs) {
                        classPath.add(jarInput.getFile())
                        String name = HashUtil.createHash(jarInput.getFile(), "MD5").asHexString()
                        File output = transformInvocation.outputProvider.getContentLocation(
                                name, jarInput.getContentTypes(),
                                jarInput.getScopes(), Format.JAR);
                        if (mCareScopes.containsAll(jarInput.getScopes())) {
                            processFileList.add(output);
                        }
                        FileUtils.copyFile(jarInput.getFile(), output);
                    }

                    Collection<DirectoryInput> directoryInputs = transformInput.getDirectoryInputs();
                    for (DirectoryInput directoryInput : directoryInputs) {
                        classPath.add(directoryInput.getFile())
                        File output = transformInvocation.outputProvider.getContentLocation(
                                directoryInput.getName(), directoryInput.getContentTypes(),
                                directoryInput.getScopes(), Format.DIRECTORY);
                        if (mCareScopes.containsAll(directoryInput.getScopes())) {
                            processFileList.add(output);
                        }
                        FileUtils.copyDirectory(directoryInput.getFile(), output);
                    }
                }
                classPath.addAll(baseVariant.androidBuilder.computeFullBootClasspath())
                SamplerInjecter.setClassPath(classPath);
                for (File processFile : processFileList) {
                    SamplerInjecter.processClassPath(processFile,
                            IncludeUtils.fomartPath(mIncludePackages),
                            IncludeUtils.fomartPath(mExcludePackages),
                            IncludeUtils.fomartPath(mExcludeClasses));
                }
            }
        });

        project.afterEvaluate({
            handleConfigChanged(project, block)
        })
    }

    static boolean isDebug(TransformInvocation transformInvocation) {
        String path = transformInvocation.getContext().getPath()
        String debug = "Debug"
        return path != null && debug.equalsIgnoreCase(path.substring(path.length() - debug.length(), path.length()))
    }

    void setCareScope(Scope scope) {
        mCareScopes.clear()
        if (scope.project) {
            mCareScopes.add(QualifiedContent.Scope.PROJECT)
        }
        if (scope.projectLocalDep) {
            mCareScopes.add(QualifiedContent.Scope.PROJECT_LOCAL_DEPS)
        }
        if (scope.subProject) {
            mCareScopes.add(QualifiedContent.Scope.SUB_PROJECTS)
        }
        if (scope.subProjectLocalDep) {
            mCareScopes.add(QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS)
        }
        if (scope.externalLibraries) {
            mCareScopes.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
        }
    }

    void setFilter(BlockCanaryExExtension block) {
        mExcludePackages.addAll(block.excludePackages)
        mIncludePackages.addAll(block.includePackages)
        mExcludeClasses.addAll(block.excludePackages)
    }

    static void handleConfigChanged(Project project, BlockCanaryExExtension block) {
        File preConfigFile = new File(project.getBuildDir().absolutePath + "${I}BlockCanaryEx${I}config.properties");
        String nowHash = block.generateHash()
        Properties properties = new Properties()
        if (!preConfigFile.exists()) {
            cleanTransformsDir(project)
        } else {
            properties.load(preConfigFile.newDataInputStream())
            String preHash = properties.getProperty("hash")
            if (!nowHash.equals(preHash)) {
                cleanTransformsDir(project)
            }
        }
        preConfigFile.delete()
        FileUtils.touch(preConfigFile)
        properties.put("hash", nowHash)
        properties.store(preConfigFile.newDataOutputStream(), "")
    }

    static void cleanTransformsDir(Project project) {
        File transformsDir = new File(project.getBuildDir().absolutePath + "${I}intermediates${I}transforms${I}${TRANSFORM_NAME}")
        FileUtils.deleteDirectory(transformsDir)
    }

    static void obtainProjectClassPath(Project project, Set<String> classPath) {
        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)

        JavaCompile javaCompile = null;
        BaseVariantData baseVariantData = null;
        if (hasApp) {
            ApplicationVariant applicationVariant = project.android.applicationVariants.getAt(0)
            javaCompile = applicationVariant.javaCompile;
            baseVariantData = applicationVariant.variantData;
        } else if (hasLib) {
            LibraryVariant libraryVariant = project.android.getLibraryVariants().getAt(0);
            javaCompile = libraryVariant.javaCompile;
            baseVariantData = libraryVariant.variantData;
        }

        if (javaCompile != null) {
            classPath.addAll(javaCompile.classpath.files)
        }

        if (baseVariantData != null) {
            List<AndroidLibrary> androidLibraries = baseVariantData.variantDependency.getCompileDependencies().androidDependencies
            for (AndroidLibrary library : androidLibraries) {
                String projectName = library.getProject()
                if (projectName != null) {
                    Set<Project> allProject = project.getRootProject().allprojects
                    for (Project p : allProject) {
                        if (p == project) {
                            continue
                        }
                        if (":" + p.name == projectName) {
                            obtainProjectClassPath(p, classPath)
                            break
                        }
                    }
                }
            }
        }
    }
}
