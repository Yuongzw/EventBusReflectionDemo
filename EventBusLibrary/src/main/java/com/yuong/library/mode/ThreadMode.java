package com.yuong.library.mode;

public enum  ThreadMode {
    //事件处理和事件发送在相同进程
    POSTING,
    //事件处理在UI线程
    MAIN,
    //后台线程，处理如保存到数据库等操作
    BACKGROUND,
    //异步执行，另起线程操作，事件处理会在单独的线程中执行，主要用于后台线程中执行耗时操作
    ASYNC
}
