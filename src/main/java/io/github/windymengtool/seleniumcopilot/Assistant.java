package io.github.windymengtool.seleniumcopilot;

import cn.hutool.core.lang.id.NanoId;
import cn.hutool.core.util.StrUtil;

public class Assistant {
    public static String nanoIdUpperCase() {
        return NanoId.randomNanoId(null, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray(), 6);
    }

    public static void errPrintWrapper(String msg) {
        String repeat = StrUtil.repeat('=', 140);
        System.err.println(repeat);
        System.err.println(msg);
        System.err.println(repeat);
        System.err.flush();
    }

    public static void errPrintFlush(String msg) {
        System.err.println(msg);
        System.err.flush();
    }
}
