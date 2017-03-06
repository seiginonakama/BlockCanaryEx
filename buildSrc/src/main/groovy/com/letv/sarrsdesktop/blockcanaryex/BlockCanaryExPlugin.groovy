package com.letv.sarrsdesktop.blockcanaryex

import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * author: zhoulei date: 2017/2/28.
 */
public class BlockCanaryExPlugin implements Plugin<Project> {
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

    private List<String> mExcludePackages = ["com.letv.sarrsdesktop.blockcanaryex.jrt", "javassist"];

    private List<String> mExcludeClasses = [];

    private List<String> mIncludePackages = [];

    @Override
    void apply(Project project) {
        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)

        if (!hasApp && !hasLib) {
            throw new IllegalStateException("'android' or 'android-library' plugin required.")
        }

        project.extensions.create("block", BlockCanaryExExtension)

        BlockCanaryExExtension block = project.block;
        project.android.registerTransform(new Transform() {
            @Override
            String getName() {
                return "blockCanaryEx";
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
                return true;
            }

            @Override
            public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
                boolean isDebug = isDebug(project)
                if ((isDebug && !block.debugEnabled)
                        || (!isDebug && !block.releaseEnabled)) {
                    mCareScopes.clear();
                    println("block canary ex disabled")
                } else {
                    setFilter(block)
                    setCareScope(block.getScope())
                }

                Collection<TransformInput> transformInputs = transformInvocation.getInputs();
                List<File> processFileList = new ArrayList<>();
                Set<File> classPath = new HashSet<>()
                ApplicationVariantImpl applicationVariant = project.android.applicationVariants.getAt(0);
                for (TransformInput transformInput : transformInputs) {
                    Collection<JarInput> jarInputs = transformInput.getJarInputs();
                    for (JarInput jarInput : jarInputs) {
                        classPath.add(jarInput.getFile())
                        File output = transformInvocation.outputProvider.getContentLocation(
                                jarInput.getName(), jarInput.getContentTypes(),
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
                classPath.addAll(applicationVariant.androidBuilder.computeFullBootClasspath())
                SamplerInjecter.setClassPath(classPath);
                for (File processFile : processFileList) {
                    SamplerInjecter.processClassPath(processFile,
                            IncludeUtils.fomartPath(mIncludePackages),
                            IncludeUtils.fomartPath(mExcludePackages),
                            IncludeUtils.fomartPath(mExcludeClasses));
                }
            }
        });
    }

    static boolean isDebug(Project project) {
        Gradle gradle = project.getGradle()
        String tskReqStr = gradle.getStartParameter().getTaskRequests().toString()

        Pattern pattern;

        if (tskReqStr.contains("assemble"))
            pattern = Pattern.compile("assemble.*(Release|Debug)")
        else
            pattern = Pattern.compile("generate.*(Release|Debug)")

        Matcher matcher = pattern.matcher(tskReqStr)

        if (matcher.find()) {
            return matcher.group(1).equals("Debug")
        } else {
            println "NO MATCH FOUND"
            return false;
        }
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
}
