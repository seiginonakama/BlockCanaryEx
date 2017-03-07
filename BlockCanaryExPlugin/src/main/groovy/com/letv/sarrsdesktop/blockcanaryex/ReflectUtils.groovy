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
