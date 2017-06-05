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
package com.letv.sarrsdesktop.blockcanaryex.jrt;

/**
 * author: zhoulei date: 2017/2/28.
 */
public class MethodInfo {
    public long getCostRealTimeMs() {
        return 0;
    }

    public long getCostRealTimeNano() {
        return 0;
    }

    public void setCostRealTimeNano(long costRealTimeNano) {
    }

    public long getCostThreadTime() {
        return 0;
    }

    public void setCostThreadTime(long costThreadTime) {
    }

    public String getClassName() {
        return null;
    }

    public String getClassSimpleName() {
        return null;
    }

    public void setCls(String cls) {
    }

    public void setMethod(String method) {
    }

    public void setParamTypes(String paramTypes) {
    }

    public String getMethodName() {
        return null;
    }

    public String getSimpleParamTypes() {
        return null;
    }

    public String getParamTypes() {
        return null;
    }

    public boolean isConstructor() {
        return false;
    }

    public boolean isClassInitializer() {
        return false;
    }

    public String generateMethodInfo(boolean simpleParamType) {
        return null;
    }
}
