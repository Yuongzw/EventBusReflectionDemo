package com.yuong.eventbus.compiler.utils;

public class Constants {

    //注解处理器中支持的注解类型
    public static final String SUBCRIBE_ANNOTATION_TYPES = "com.yuong.eventbus.annotation.Subscribe";

    //apt 生成类文件所属的包名
    public static final String PACKAGE_NAME = "packageName";

    //apt 生成类文件的类名
    public static final String CLASS_NAME = "className";

    //所有的事件订阅方法。生成索引接口
    public static final String SUBSCRIBERINFO_INDEX = "com.yuong.eventbus.annotation.SubscriberInfoIndex";

    //全局属性名
    public static final String FIELD_NAME = "SUBSCRIBER_INDEX";

    //putIndex方法的参数对象名
    public static final String PUTINDEX_PARAMETER_NAME = "info";

    //加入 map集合方法名
    public static final String PUTINDEX_METHOD_NAME = "putIndex";

    //getSubscribeInfo 方法的参数对象名
    public static final String GETSUBSCRIBEINFO_PARAMETER_NAME = "subscriberClass";

    //通过订阅者（MainActivity）获取所有的订阅方法的方法名
    public static final String GETSUBSCRIBERINFO_METHOD_NAME = "getSubscriberInfo";
}
