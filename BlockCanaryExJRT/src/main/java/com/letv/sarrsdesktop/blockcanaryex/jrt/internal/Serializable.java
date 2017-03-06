package com.letv.sarrsdesktop.blockcanaryex.jrt.internal;

/**
 * author: zhoulei date: 2017/3/2.
 */

public interface Serializable {
    void deserialize(String src) throws SerializeException;
    String serialize() throws SerializeException;
}
