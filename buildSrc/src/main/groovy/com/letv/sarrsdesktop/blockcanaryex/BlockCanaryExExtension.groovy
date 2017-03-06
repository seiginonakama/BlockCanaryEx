package com.letv.sarrsdesktop.blockcanaryex

import org.gradle.api.Action

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
}
