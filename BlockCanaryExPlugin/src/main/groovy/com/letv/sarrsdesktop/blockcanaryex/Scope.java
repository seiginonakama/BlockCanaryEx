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
