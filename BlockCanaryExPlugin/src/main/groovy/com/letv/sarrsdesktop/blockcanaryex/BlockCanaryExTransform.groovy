package com.letv.sarrsdesktop.blockcanaryex

import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.internal.hash.HashUtil

/**
 * author: zhoulei date: 2017/5/23.
 */
public class BlockCanaryExTransform extends Transform {
    private static final String TRANSFORM_NAME = "blockCanaryEx";
    private static final Set<QualifiedContent.ContentType> TYPES = new HashSet<>();
    private final Set<QualifiedContent.Scope> mScopes;
    private final Set<QualifiedContent.Scope> mCareScopes;
    private final BlockCanaryExExtension mExtension;
    private final Project mProject;
    private final File mJarCacheDir;
    private final def mHasApp;
    private final def mHasLib;

    private List<String> mExcludePackages = ["com.letv.sarrsdesktop.blockcanaryex.jrt.internal", "javassist"];

    private List<String> mExcludeClasses = ["com.letv.sarrsdesktop.blockcanaryex.jrt.BlockInfo", "com.letv.sarrsdesktop.blockcanaryex.jrt.BlockCanaryEx",
                                            "com.letv.sarrsdesktop.blockcanaryex.jrt.Config", "com.letv.sarrsdesktop.blockcanaryex.jrt.FrequentMethodInfo",
                                            "com.letv.sarrsdesktop.blockcanaryex.jrt.MethodInfo"];

    private List<String> mIncludePackages = [];

    private static final String I = File.separator;

    static {
        TYPES.add(QualifiedContent.DefaultContentType.CLASSES);
    }

    BlockCanaryExTransform(Project project, File buildDir,
                           def hasApp,
                           def hasLib, Set<QualifiedContent.Scope> scopes, BlockCanaryExExtension extension) {
        mProject = project;
        mScopes = Collections.unmodifiableSet(scopes);
        mExtension = extension;
        mJarCacheDir = new File(buildDir, "jar-cache");
        mCareScopes = new HashSet<>()

        mHasApp = hasApp;
        mHasLib = hasLib;
    }

    @Override
    public String getName() {
        return TRANSFORM_NAME;
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TYPES;
    }

    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        return mScopes;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation
                                  transformInvocation) throws TransformException, InterruptedException, IOException {
        boolean enable = true;

        if (mHasLib && !mExtension.releaseEnabled) {
            //library only has release status
            println("BlockCanaryEx only enable when library project releaseEnabled = true")
            enable = false;
        } else {
            boolean isDebug = isDebug(transformInvocation)
            if ((isDebug && !mExtension.debugEnabled)
                    || (!isDebug && !mExtension.releaseEnabled)) {
                enable = false;
                println("block canary ex disabled")
            }
        }

        if (!enable) {
            mCareScopes.clear()
        } else {
            setFilter(mExtension)
            setCareScope(mExtension.getScope())
        }

        cleanTransformsDir(mProject)

        Collection<TransformInput> transformInputs = transformInvocation.getInputs();
        Set<File> classPath = new HashSet<>()

        obtainProjectClassPath(mProject, classPath)

        BaseVariant baseVariant;
        if (mHasApp) {
            baseVariant = mProject.android.applicationVariants.getAt(0)
        } else {
            baseVariant = mProject.android.getLibraryVariants().getAt(0)
        }

        for (TransformInput transformInput : transformInputs) {
            for (JarInput jarInput : transformInput.getJarInputs()) {
                classPath.add(jarInput.getFile())
            }

            for (DirectoryInput directoryInput : transformInput.getDirectoryInputs()) {
                classPath.add(directoryInput.getFile())
            }
        }

        classPath.addAll(baseVariant.androidBuilder.computeFullBootClasspath())

        SamplerInjecter.setClassPath(classPath);

        for (TransformInput transformInput : transformInputs) {
            for (JarInput jarInput : transformInput.getJarInputs()) {
                File input = jarInput.getFile();
                File tmpFile = null;
                String name = HashUtil.createHash(input, "MD5").asHexString()
                if (mCareScopes.containsAll(jarInput.getScopes())) {
                    File cache = findCachedJar(name)
                    if (cache != null) {
                        input = cache
                    } else {
                        File tmpDir = transformInvocation.context.temporaryDir;
                        if (!tmpDir.isDirectory()) {
                            if (tmpDir.exists()) {
                                tmpDir.delete();
                            }
                        }
                        tmpDir.mkdirs()
                        tmpFile = new File(tmpDir, name + ".jar")
                        FileUtils.copyFile(input, tmpFile)
                        input = tmpFile
                        injectSampler(input)
                        cacheProcessedJar(input, name)
                    }
                }
                File output = transformInvocation.outputProvider.getContentLocation(
                        name, jarInput.getContentTypes(),
                        jarInput.getScopes(), Format.JAR);
                FileUtils.copyFile(input, output);
                if (tmpFile != null) {
                    tmpFile.delete()
                }
            }

            for (DirectoryInput directoryInput : transformInput.getDirectoryInputs()) {
                File output = transformInvocation.outputProvider.getContentLocation(
                        directoryInput.getName(), directoryInput.getContentTypes(),
                        directoryInput.getScopes(), Format.DIRECTORY);
                if (mCareScopes.containsAll(directoryInput.getScopes())) {
                    injectSampler(directoryInput.file)
                }
                FileUtils.copyDirectory(directoryInput.file, output)
            }
        }
    }

    File findCachedJar(String md5) {
        if (!mJarCacheDir.isDirectory()) {
            if (mJarCacheDir.exists()) {
                mJarCacheDir.delete()
            }
            return null;
        }
        String target = md5 + ".jar"
        String[] files = mJarCacheDir.list()
        for (String name : files) {
            if (target == name) {
                return new File(mJarCacheDir, target)
            }
        }
        return null
    }

    void cacheProcessedJar(File jar, String md5) {
        if (!mJarCacheDir.isDirectory()) {
            if (mJarCacheDir.exists()) {
                mJarCacheDir.delete()
            }
        }
        if (!mJarCacheDir.exists()) {
            mJarCacheDir.mkdirs()
        }
        FileUtils.copyFile(jar, new File(mJarCacheDir, md5 + ".jar"))
    }

    void injectSampler(File path) {
        SamplerInjecter.processClassPath(path,
                IncludeUtils.fomartPath(mIncludePackages),
                IncludeUtils.fomartPath(mExcludePackages),
                IncludeUtils.fomartPath(mExcludeClasses));
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
        mExcludeClasses.addAll(block.excludeClasses)
    }

    static void obtainProjectClassPath(Project project, Set<String> classPath) {
        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)

        JavaCompile javaCompile = null;
        if (hasApp) {
            ApplicationVariant applicationVariant = project.android.applicationVariants.getAt(0)
            javaCompile = applicationVariant.javaCompile;
        } else if (hasLib) {
            LibraryVariant libraryVariant = project.android.getLibraryVariants().getAt(0);
            javaCompile = libraryVariant.javaCompile;
        }

        if (javaCompile != null) {
            classPath.addAll(javaCompile.classpath.files)
        }
    }

    static void cleanTransformsDir(Project project) {
        File transformsDir = new File(project.getBuildDir().absolutePath + "${I}intermediates${I}transforms${I}${TRANSFORM_NAME}")
        FileUtils.deleteDirectory(transformsDir)
    }
}
