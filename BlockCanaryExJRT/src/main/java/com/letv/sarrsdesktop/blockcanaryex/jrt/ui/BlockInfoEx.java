package com.letv.sarrsdesktop.blockcanaryex.jrt.ui;

import com.letv.sarrsdesktop.blockcanaryex.jrt.BlockInfo;
import com.letv.sarrsdesktop.blockcanaryex.jrt.internal.SerializeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * author: zhoulei date: 2017/3/2.
 */

public class BlockInfoEx extends BlockInfo {
    public File logFile;

    public static BlockInfoEx newInstance(File logFile) throws IOException, ClassNotFoundException, SerializeException {
        Long fileLength = logFile.length();
        byte[] content = new byte[fileLength.intValue()];
        FileInputStream in = null;
        try {
            in = new FileInputStream(logFile);
            in.read(content);
            in.close();
        } finally {
            if(in != null) {
                in.close();
            }
        }

        String src = new String(content, "UTF-8");
        BlockInfoEx blockInfoEx = new BlockInfoEx();
        blockInfoEx.logFile = logFile;
        blockInfoEx.deserialize(src);
        return blockInfoEx;
    }
}
