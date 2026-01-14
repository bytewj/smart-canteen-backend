package com.sky.constant;

public class DelayTimeConstant {
    // 总共 13分钟 = 10s + 10s + 40s + 5min + 7min
    public static final int[] DELAY_TIMES = {
            5_000,  // 10s
            15_000,  // 10s
            40_000,  // 40s
            300_000, // 5min
            540_000  // 7min
    };
}

