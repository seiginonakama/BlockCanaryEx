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

import java.lang.reflect.Field;

/**
 * author: zhoulei date: 2017/3/7.
 */
public class ReflectUtils {
    public static final String KV = " = "
    public static final String SEPARATOR = "\r\n";

    public static String printObject(def object) {
        if(object == null) {
            return String.valueOf(object);
        }

        StringBuilder content = new StringBuilder();
        Field[] fields = object.getClass().getDeclaredFields();
        for(Field field : fields) {
            field.setAccessible(true)
            content.append(field.name).append(KV).append(field.get(object)).append(SEPARATOR)
        }
        return content.toString();
    }
}
