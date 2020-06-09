package com.yuong.eventbus.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.yuong.eventbus.annotation.Subscribe;
import com.yuong.eventbus.compiler.utils.Constants;
import com.yuong.eventbus.compiler.utils.EmptyUtils;
import com.yuong.eventbus.annotation.mode.EventBeans;
import com.yuong.eventbus.annotation.mode.SubscriberInfo;
import com.yuong.eventbus.annotation.mode.SubscriberMethod;
import com.yuong.eventbus.annotation.mode.ThreadMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

//用来生成 META-INF/service/javax-annotation
@AutoService(Processor.class)
//支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes({Constants.SUBCRIBE_ANNOTATION_TYPES})
//指定JDK的版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
//注解处理器接收的参数
@SupportedOptions({Constants.PACKAGE_NAME, Constants.CLASS_NAME})
public class SubscriberProcessor extends AbstractProcessor {

    //操作 Element 工具类（类、函数、属性这些都属于 Element）
    private Elements elementUtils;

    //type(类信息)工具类，包含用于操作 TypeMirror 的工具方法
    private Types typeUtils;

    //Messager 用来警告错误，警告其他提示信息
    private Messager messager;

    //文件生成器，类/资源 filer用来创建新的类文件 .class文件及辅助文件
    private Filer filer;

    //APT 包名
    private String packageName;

    //APT 类名
    private String className;

    //临时 map 存储，用来存放订阅方法信息
    //key:MainActivity  value:MainActivity中所有的订阅方法
    private Map<TypeElement, List<ExecutableElement>> methodsByClass = new HashMap<>();

    /**
     * 该方法主要用于一些初始化操作，通过该方法的参数 RoundEnvironment 可以获取有用的工具类
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        //初始化
        elementUtils = processingEnv.getElementUtils();
        typeUtils = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
        filer = processingEnv.getFiler();
        //通过 ProcessingEnvironment 去获取对应的参数
        Map<String, String> options = processingEnv.getOptions();
        if (!EmptyUtils.isEmpty(options)) {
            packageName = options.get(Constants.PACKAGE_NAME);
            className = options.get(Constants.CLASS_NAME);
            messager.printMessage(Diagnostic.Kind.NOTE,
                    "packageName:" + packageName + ", className:" + className);
        }
        if (EmptyUtils.isEmpty(packageName) || EmptyUtils.isEmpty(className)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "注解处理器需要的参数为空，请在对应的build.gradle文件中配置参数");
        }
    }


    /**
     * 开始处理注解
     *
     * @param annotations 支持处理注解的节点集合
     * @param roundEnv    当前或是之前的运行环境，可以通过该对象查找注解
     * @return true 表示后续处理器不会再处理，或以处理完成
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!EmptyUtils.isEmpty(annotations)) {
            //获取所有被 @Subscribe 注解的元素集合
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(Subscribe.class);
            if (!EmptyUtils.isEmpty(elements)) {
                try {
                    //解析元素
                    parseElements(elements);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        }
        return false;
    }

    private void parseElements(Set<? extends Element> elements) {
        for (Element element : elements) {
            //@Subscribe 注解使用在方法之上（尽量避免用 instanceof 进行判断）
            if (element.getKind() != ElementKind.METHOD) {
                messager.printMessage(Diagnostic.Kind.ERROR, "仅解析@Subscribe在方法上的元素");
                return;
            }
            //强转为方法元素
            ExecutableElement method = (ExecutableElement) element;
            //检查方法，条件：订阅方法必须是非静态的，公开的，参数只有一个
            if (checkHasNoErrors(method)) {
                //获取封装订阅方法的类(方法上一个节点)
                TypeElement classElement = (TypeElement) method.getEnclosingElement();
                //以类名为key，保存订阅方法
                List<ExecutableElement> methods = methodsByClass.get(classElement);
                if (methods == null) {
                    methods = new ArrayList<>();
                    methodsByClass.put(classElement, methods);
                }
                methods.add(method);
            }
            messager.printMessage(Diagnostic.Kind.NOTE, "遍历注解的方法：" + method.getSimpleName().toString());
        }
        //通过 Element 工具类，获取 subscriberIndex类型
        TypeElement subscriberIndexType = elementUtils.getTypeElement(Constants.SUBSCRIBERINFO_INDEX);

        //生成类文件
        try {
            if(subscriberIndexType != null) {
                createFile(subscriberIndexType);
            }else {
                messager.printMessage(Diagnostic.Kind.NOTE, "subscriberIndexType 为空！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
        }
    }

    private boolean checkHasNoErrors(ExecutableElement method) {
        if (method.getModifiers().contains(Modifier.STATIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "订阅事件方法不能是 static 静态方法");
            return false;
        }
        if (!method.getModifiers().contains(Modifier.PUBLIC)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "订阅事件方法必须是public修饰的方法");
            return false;
        }
        List<? extends VariableElement> parameters = method.getParameters();
        if (parameters != null && parameters.size() != 1) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                    "订阅事件方法有且只有一个参数");
            return false;
        }
        return true;
    }

    private void createFile(TypeElement subscriberIndexType) throws Exception {
        //添加静态代码块 SUBSCRIBER_INDEX = new HashMap<Class, SubscriberInfo>();
        CodeBlock.Builder codeBlock = CodeBlock.builder();
        codeBlock.addStatement("$N = new $T<$T, $T>()",
                Constants.FIELD_NAME,
                HashMap.class,
                Class.class,
                SubscriberInfo.class
        );

        //双层遍历，第一层遍历@Subscribe注解的方法所属类，第二层遍历每个类中的搜订阅方法
        for (Map.Entry<TypeElement, List<ExecutableElement>> entry
                : methodsByClass.entrySet()) {
            CodeBlock.Builder contentBlock = CodeBlock.builder();
            CodeBlock contentCode = null;
            String format;
            for (int i = 0; i < entry.getValue().size(); i++) {
                //获取每个方法上的  Subscribe 注解值
                Subscribe subscribe = entry.getValue().get(i).getAnnotation(Subscribe.class);
                //获取订阅事件方法所有参数
                List<? extends VariableElement> parameters = entry.getValue().get(i).getParameters();
                //获取订阅事件的方法名
                String methodName = entry.getValue().get(i).getSimpleName().toString();
                //参数类型
                TypeElement parameterElement = (TypeElement) typeUtils.asElement(parameters.get(0).asType());
                //如果是最后一个添加，则无需逗号结尾
                if (i == entry.getValue().size() - 1) {
                    format = "new $T($T.class, $S. $T.class, $T, $L, $L, $L)";
                } else {
                    format = "new $T($T.class, $S. $T.class, $T, $L, $L, $L),\n";
                }
                contentCode = contentBlock.add(format,
                        SubscriberMethod.class,
                        ClassName.get(entry.getKey()),
                        methodName,
                        ClassName.get(parameterElement),
                        ThreadMode.class,
                        subscribe.threadMode(),
                        subscribe.priority(),
                        subscribe.sticky()).build();

                if (contentCode != null) {
                    //putIndex(new EventBeans(MainActivity.class, new SubscriberMethod[]{})
                    codeBlock.beginControlFlow("putIndex(new $T($T.class, new $T[]",
                            EventBeans.class,
                            ClassName.get(entry.getKey()),
                            SubscriberMethod.class)
                            .add(contentCode)
                            .endControlFlow("))");


                } else {
                    messager.printMessage(Diagnostic.Kind.ERROR, "注解处理器双层循环发生错误！");
                }
            }
            //全局属性：Map<Class<?>, SubscriberInfo>
            TypeName fieldType = ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(Class.class),
                    ClassName.get(SubscriberInfo.class));

            //putIndex方法参数：putIndex(SubscriberInfo info)
            ParameterSpec putIndexParameter = ParameterSpec.builder(
                    ClassName.get(SubscriberInfo.class),
                    Constants.PUTINDEX_PARAMETER_NAME).build();

            //putIndex方法配置：private static void putIndex (SubscriberInfo info) {

            MethodSpec.Builder putIndexBuilder = MethodSpec
                    .methodBuilder(Constants.PUTINDEX_METHOD_NAME)//方法名
                    .addModifiers(Modifier.PRIVATE, Modifier.STATIC)//private static 修饰
                    .addParameter(putIndexParameter);//添加方法参数

            //putIndex方法内容：SUBSCRIBER_INDEX.put(info.getSubscriberClass(), info)
            putIndexBuilder.addStatement("$N.put($N.getSubscriberClass(), $N)",
                    Constants.FIELD_NAME,
                    Constants.PUTINDEX_PARAMETER_NAME,
                    Constants.PUTINDEX_PARAMETER_NAME).build();

            //getSubscriberInfo 方法参数： Class subscriberClass
            ParameterSpec getSubscriberInfoParameter = ParameterSpec.builder(
                    ClassName.get(Class.class),
                    Constants.GETSUBSCRIBEINFO_PARAMETER_NAME
            ).build();

            //getSubscriberInfo方法配置：public SubscriberInfo getSubscriberInfo(Class<?> subscriberClass) {
            MethodSpec.Builder getSubscriberInfoBuilder = MethodSpec
                    .methodBuilder(Constants.GETSUBSCRIBERINFO_METHOD_NAME)//方法名
                    .addAnnotation(Override.class)//重写方法注解
                    .addModifiers(Modifier.PUBLIC)//public修饰符
                    .addParameter(getSubscriberInfoParameter)//方法参数
                    .returns(SubscriberInfo.class);//返回值


            //getSubscriberInfo 方法内容：return SUBSCRIBER_INDEX.get(subscriberClass);
            getSubscriberInfoBuilder.addStatement("return $N.get($N)",
                    Constants.FIELD_NAME,
                    Constants.GETSUBSCRIBEINFO_PARAMETER_NAME
            );

            TypeSpec typeSpec = TypeSpec.classBuilder(className)
                    //实现 SubscriberInfoIndex接口
                    .addSuperinterface(ClassName.get(subscriberIndexType))
                    //该类的修饰符
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    //添加静态块
                    .addStaticBlock(codeBlock.build())
                    //全局属性 private static final Map>Class<?>, SubscriberMethod> SUBSCRIBER_INDEX
                    .addField(fieldType, Constants.FIELD_NAME, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    //第一个方法：加入全局 map 集合
                    .addMethod(putIndexBuilder.build())
                    //第二个方法“通过订阅者对象（MainActivity）获取所有的订阅方法
                    .addMethod(getSubscriberInfoBuilder.build())
                    .build();
            //生成类文件：EventBusIndex
            JavaFile.builder(
                    //包名
                    packageName,
                    typeSpec)
                    .build().writeTo(filer);
        }

    }

}
