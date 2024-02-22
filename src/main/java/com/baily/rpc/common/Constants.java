package com.baily.rpc.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * baily
 */
public class Constants {
    public static ByteBuf DELIMITER = Unpooled.copiedBuffer("$$".getBytes());
    public static String DELIMITER_STR = "$$";
}
