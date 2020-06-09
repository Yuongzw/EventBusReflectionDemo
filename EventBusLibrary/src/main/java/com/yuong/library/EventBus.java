package com.yuong.library;

import android.os.Handler;
import android.os.Looper;

import com.yuong.library.annotation.Subscribe;
import com.yuong.library.core.MethodManager;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EventBus {

    private static volatile EventBus instance;

    //用来保存带注解的方法
    private Map<Object, List<MethodManager>> cacheMap;

    private Handler handler;

    private ExecutorService executorService;

    private EventBus() {
        cacheMap = new HashMap<>();
        //用来在主线程调度
        handler = new Handler(Looper.getMainLooper());
        //创建一个子线程
        executorService = Executors.newCachedThreadPool();
    }

    public static EventBus getDefault() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }

    //订阅
    public void register(Object getter) {
        List<MethodManager> methodLsit = cacheMap.get(getter);
        if (methodLsit == null) {
            methodLsit = findAnnotationMethod(getter);
            if (methodLsit != null && methodLsit.size() > 0) {
                cacheMap.put(getter, methodLsit);
            }
        }
    }

    public void unRegister(Object getter) {
        cacheMap.remove(getter);
    }

    public boolean isRegister(Object getter) {
       return cacheMap.containsKey(getter);
    }

    private List<MethodManager> findAnnotationMethod(Object getter) {
        List<MethodManager> methodLsit = new ArrayList<>();
        Class<?> clazz = getter.getClass();
        Method[] methods = clazz.getMethods();
        while (clazz != null) {
            String className = clazz.getName();
            if (className.startsWith("java.") || className.startsWith("javax.") ||
                    className.startsWith("android.") || className.startsWith("androidx.")) {
                break;
            }
            for (Method method : methods) {
                Subscribe subscribe = method.getAnnotation(Subscribe.class);
                if (subscribe == null) {
                    continue;
                }
                Type returnType = method.getGenericReturnType();
                if (!"void".equals(returnType.toString())) {
                    throw new RuntimeException(method.getName() + " 方法返回值必须是void类型");
                }
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length > 1) {
                    throw new RuntimeException(method.getName() + " 方法有且只有一个参数");
                }
                //符合要求
                MethodManager manager = new MethodManager(parameterTypes[0], subscribe.threadMode(), method);
                methodLsit.add(manager);
            }
            //不断循环寻找父类，是否含有订阅者
            clazz = clazz.getSuperclass();
        }
        return methodLsit;
    }

    public void post(final Object setter) {
        Set<Object> set = cacheMap.keySet();
        for (final Object getter : set) {
            //获取 MainActivity 中所有注解的方法
            List<MethodManager> methodList = cacheMap.get(getter);
            if (methodList != null && methodList.size() > 0) {
                for (final MethodManager method : methodList) {
                    //通过 EventBean来判断是否匹配上
                    if (method.getType().isAssignableFrom(setter.getClass())) {
                        //线程调度
                        switch (method.getThreadMode()) {
                            case MAIN:
                                if (Looper.myLooper() == Looper.getMainLooper()) {
                                    invoke(method, getter, setter);
                                } else {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            invoke(method, getter, setter);
                                        }
                                    });
                                }
                                break;
                            case POSTING:
                                invoke(method, getter, setter);
                                break;
                            case BACKGROUND:
                                if (Looper.myLooper() == Looper.getMainLooper()) {
                                    executorService.execute(new Runnable() {
                                        @Override
                                        public void run() {
                                            invoke(method, getter, setter);
                                        }
                                    });
                                } else {
                                    invoke(method, getter, setter);
                                }
                                break;
                            case ASYNC:
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        invoke(method, getter, setter);
                                    }
                                });
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
    }

    private void invoke(MethodManager method, Object getter, Object setter) {
        Method execute = method.getMethod();
        try {
            execute.invoke(getter, setter);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}
