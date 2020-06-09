package com.yuong.eventbus.annotation.mode;

import java.lang.reflect.Method;

public class SubscriberMethod {

    private String methodName;  //订阅方法名
    private Method method;  //订阅方法
    private ThreadMode threadMode;  //线程模式
    private Class<?> evnentType; //参数类型
    private int priority;   //优先级
    private boolean sticky; //粘性事件

    public SubscriberMethod(Class subscriberClass, String methodName, ThreadMode threadMode, Class<?> evnentType, int priority, boolean sticky) {
        this.methodName = methodName;
        this.threadMode = threadMode;
        this.evnentType = evnentType;
        this.priority = priority;
        this.sticky = sticky;
        try {
            method = subscriberClass.getDeclaredMethod(methodName, evnentType);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public ThreadMode getThreadMode() {
        return threadMode;
    }

    public void setThreadMode(ThreadMode threadMode) {
        this.threadMode = threadMode;
    }

    public Class<?> getEvnentType() {
        return evnentType;
    }

    public void setEvnentType(Class<?> evnentType) {
        this.evnentType = evnentType;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean isSticky() {
        return sticky;
    }

    public void setSticky(boolean sticky) {
        this.sticky = sticky;
    }
}
