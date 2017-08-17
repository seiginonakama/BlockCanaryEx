/*
 * Copyright (C) 2017 seiginonakama (https://github.com/seiginonakama).
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

import com.android.build.api.transform.QualifiedContent
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.builder.Version
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * author: zhoulei date: 2017/2/28.
 */
public class BlockCanaryExPlugin implements Plugin<Project> {
    private static final Set<QualifiedContent.Scope> SCOPES = new HashSet<>();
    static {
        SCOPES.add(QualifiedContent.Scope.PROJECT);
        SCOPES.add(QualifiedContent.Scope.PROJECT_LOCAL_DEPS);
        SCOPES.add(QualifiedContent.Scope.SUB_PROJECTS);
        SCOPES.add(QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS);
        SCOPES.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES);
    }

    static final boolean ABOVE_ANDROID_GRADLE_PLUGIN_3;
    static {
        String aVersion = Version.ANDROID_GRADLE_PLUGIN_VERSION.trim();
        char[] aVersionChars = aVersion.chars
        if (aVersionChars.length == 0) {
            ABOVE_ANDROID_GRADLE_PLUGIN_3 = false;
        } else {
            char firstChar = aVersion.charAt(0)
            if (aVersionChars.length == 1) {
                ABOVE_ANDROID_GRADLE_PLUGIN_3 = firstChar >= '3';
            } else {
                ABOVE_ANDROID_GRADLE_PLUGIN_3 = aVersion.charAt(1) != '.' || firstChar >= '3';
            }
        }
    }

    private File mBuildDir;
    private File mConfigFile;

    @Override
    void apply(Project project) {
        def hasApp = project.plugins.withType(AppPlugin)
        def hasLib = project.plugins.withType(LibraryPlugin)

        if (!hasApp && !hasLib) {
            throw new IllegalStateException("'android' or 'android-library' plugin required.")
        }

        if (ABOVE_ANDROID_GRADLE_PLUGIN_3) {
            //PROJECT_LOCAL_DEPS, SUB_PROJECTS_LOCAL_DEPS deprecated, replaced by EXTERNAL_LIBRARIES
            SCOPES.remove(QualifiedContent.Scope.PROJECT_LOCAL_DEPS)
            SCOPES.remove(QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS)
        }

        if (hasLib) {
            //Transforms with scopes '[SUB_PROJECTS, SUB_PROJECTS_LOCAL_DEPS, EXTERNAL_LIBRARIES]' cannot be applied to library projects.
            SCOPES.remove(QualifiedContent.Scope.SUB_PROJECTS)
            SCOPES.remove(QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS)
            SCOPES.remove(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
        }

        mBuildDir = new File(project.getBuildDir(), "BlockCanaryEx")
        mConfigFile = new File(mBuildDir, "config.properties")

        project.extensions.create("block", BlockCanaryExExtension)

        BlockCanaryExExtension block = project.block;

        project.android.registerTransform(new BlockCanaryExTransform(project, mBuildDir, hasApp, hasLib, SCOPES, block));

        project.afterEvaluate({
            handleConfigChanged(project, block)

            if (ABOVE_ANDROID_GRADLE_PLUGIN_3) {
                if (block.scope.projectLocalDep
                        || block.scope.subProjectLocalDep) {
                    //PROJECT_LOCAL_DEPS, SUB_PROJECTS_LOCAL_DEPS deprecated, replaced by EXTERNAL_LIBRARIES
                    block.scope.externalLibraries = true;
                }
                block.scope.projectLocalDep = false;
                block.scope.subProjectLocalDep = false;
            }
        })
    }

    void handleConfigChanged(Project project, BlockCanaryExExtension block) {
        String nowHash = block.generateHash()
        Properties properties = new Properties()
        if (!mConfigFile.exists()) {
            BlockCanaryExTransform.cleanTransformsDir(project)
        } else {
            properties.load(mConfigFile.newDataInputStream())
            String preHash = properties.getProperty("hash")
            if (!nowHash.equals(preHash)) {
                BlockCanaryExTransform.cleanTransformsDir(project)
            }
        }
        mConfigFile.delete()
        FileUtils.touch(mConfigFile)
        properties.put("hash", nowHash)
        properties.store(mConfigFile.newDataOutputStream(), "")
    }
}
