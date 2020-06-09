package com.yuong.eventbus.annotation.mode;

public class EventBeans {

    //订阅者对象的class，如 MainActivity.class
    private final Class subscriberClass;

    //订阅方法数组
    private final SubscriberMethod[] methods;

    public EventBeans(Class subscriberClass, SubscriberMethod[] methods) {
        this.subscriberClass = subscriberClass;
        this.methods = methods;
    }

    public Class getSubscriberClass() {
        return subscriberClass;
    }

    public synchronized SubscriberMethod[] getMethods() {
        return methods;
    }
}
