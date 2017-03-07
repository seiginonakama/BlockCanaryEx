package com.letv.sarrsdesktop.blockcanaryex;
/**
 * author: zhoulei date: 2017/3/6.
 */
public class Scope {
    boolean project = true;
    boolean projectLocalDep = false;
    boolean subProject = true;
    boolean subProjectLocalDep = false;
    boolean externalLibraries = false;

    void project(boolean enable) {
        project = enable;
    }

    void projectLocalDep(boolean enable) {
        projectLocalDep = enable;
    }

    void subProject(boolean enable) {
        subProject = enable;
    }

    void subProjectLocalDep(boolean enable) {
        subProjectLocalDep = enable;
    }

    void externalLibraries(boolean enable) {
        externalLibraries = enable;
    }
}
