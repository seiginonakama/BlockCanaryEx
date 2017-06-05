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

import org.gradle.api.Action
import org.gradle.internal.hash.HashUtil

/**
 * author: zhoulei date: 2017/3/6.
 */
class BlockCanaryExExtension {
    boolean releaseEnabled = false;
    boolean debugEnabled = true;
    List<String> includePackages = new ArrayList<>();
    List<String> excludePackages = new ArrayList<>();
    List<String> excludeClasses = new ArrayList<>();

    Scope scope = new Scope();

    void setReleaseEnabled(boolean enabled) {
        releaseEnabled = enabled;
    }

    void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    void setIncludePackages(List<String> includePackages) {
        this.includePackages.addAll(includePackages);
    }

    void setExcludePackages(List<String> excludePackages) {
        this.excludePackages.addAll(excludePackages);
    }

    void setExcludeClasses(List<String> excludeClasses) {
        this.excludeClasses.addAll(excludeClasses);
    }

    void scope(Action<Scope> action) {
        action.execute(scope);
    }

    Scope getScope() {
        return scope;
    }

    String generateHash() {
        String content = "releaseEnabled:{$releaseEnabled} debugEnabled:{$debugEnabled} includePackages:{$includePackages} excludePackages:{$excludePackages}" +
                " excludeClasses:{$excludeClasses} scope:" + ReflectUtils.printObject(scope);
        return HashUtil.createCompactMD5(content)
    }
}
