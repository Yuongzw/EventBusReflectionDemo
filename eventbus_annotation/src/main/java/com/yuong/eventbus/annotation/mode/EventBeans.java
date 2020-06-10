package com.yuong.eventbus.annotation.mode;

public class EventBeans implements SubscriberInfo{

    //订阅者对象的class，如 MainActivity.class
    private final Class subscriberClass;

    //订阅方法数组
    private final SubscriberMethod[] methods;

    public EventBeans(Class subscriberClass, SubscriberMethod[] methods) {
        this.subscriberClass = subscriberClass;
        this.methods = methods;
    }

    @Override
    public Class getSubscriberClass() {
        return subscriberClass;
    }

    @Override
    public SubscriberMethod[] getSubscriberMethods() {
        return methods;
    }
}
