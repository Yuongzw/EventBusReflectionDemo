package com.yuong.library.core;

import com.yuong.library.mode.ThreadMode;

import java.lang.reflect.Method;

public class MethodManager {

    //订阅者的回调方法（注解方法）的参数类型
    private Class<?> type;

    //订阅者的回调方法的线程模式
    private ThreadMode threadMode;

    //订阅者的回调方法
    private Method method;

    public MethodManager(Class<?> type, ThreadMode threadMode, Method method) {
        this.type = type;
        this.threadMode = threadMode;
        this.method = method;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public ThreadMode getThreadMode() {
        return threadMode;
    }

    public void setThreadMode(ThreadMode threadMode) {
        this.threadMode = threadMode;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }
}
