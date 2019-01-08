package github.xusheng.mvc.servlet;

import github.xusheng.mvc.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description 自定义 DispatchServlet
 * @Author xusheng
 * @Create 2019-01-08 22:42
 * @Version 1.0
 **/
public class MyDispatchServlet extends HttpServlet {
    //配置文件
    private Properties properties = new Properties();
    // 集合全自动扫描基础包下面的类限定名
    private List<String> classNames = new ArrayList<>();
    // 缓存 key/value: 类注解参数/类实例对象，存储controller和service实例
    private Map<String, Object> IOC = new ConcurrentHashMap<>();
    // key/value: 请求url/handler的method
    private Map<String, Method> handlerMapping = new ConcurrentHashMap<>();

    // 再维护一个map，存储controller实例
    private Map<String, Object> controllerMaps = new ConcurrentHashMap<>();

//    private Object lock = new Object();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) {
        PrintWriter out = null;
        try {
            out = resp.getWriter();
            if (handlerMapping.isEmpty()) {
                System.err.println("handlerMapping is empty");
                out.print("handlerMapping is empty");
                return;
            }
            // 如：/project_name/classURI/methodURI
            String uri = req.getRequestURI();
            // 如：/project_name
            String contextPath = req.getContextPath();

            String url = uri.replace(contextPath, "").replaceAll("/+", "/");
            Method handlerMethod = handlerMapping.get(url);
            if (null == handlerMethod) {
                out.print("404!!!the " + url + " is not exists");
                return;
            }
            // 获取方法的参数列表
            Parameter methodParameters[] = handlerMethod.getParameters();
            // 调用方法需要传递的形参
            Object paramValues[] = new Object[methodParameters.length];
            for (int i = 0; i < methodParameters.length; i++) {

                if (ServletRequest.class.isAssignableFrom(methodParameters[i].getType())) {
                    paramValues[i] = req;
                } else if (ServletResponse.class.isAssignableFrom(methodParameters[i].getType())) {
                    paramValues[i] = resp;
                } else {// 其它参数，目前只支持String，Integer，Float，Double
                    // 参数绑定的名称，默认为方法形参名
                    String bindingValue = methodParameters[i].getName();
                    if (methodParameters[i].isAnnotationPresent(MyRequestParam.class)) {
                        bindingValue = methodParameters[i].getAnnotation(MyRequestParam.class).value();
                    }
                    // 从请求中获取参数的值
                    String paramValue = req.getParameter(bindingValue);
                    paramValues[i] = paramValue;
                    if (paramValue != null) {
                        if (Integer.class.isAssignableFrom(methodParameters[i].getType())) {
                            paramValues[i] = Integer.parseInt(paramValue);
                        } else if (Float.class.isAssignableFrom(methodParameters[i].getType())) {
                            paramValues[i] = Float.parseFloat(paramValue);
                        } else if (Double.class.isAssignableFrom(methodParameters[i].getType())) {
                            paramValues[i] = Double.parseDouble(paramValue);
                        }
                    }
                }
            }
            handlerMethod.invoke(controllerMaps.get(url), paramValues);

        } catch (Exception e) {
            e.printStackTrace();

            if (null != out) {
                out.print("500!! Exception" + Arrays.toString(e.getStackTrace()));
            }

        }
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            //1.加载配置文件
            doLoadConfig(config.getInitParameter("contextConfigLocation"));

            //2.初始化所有相关联的类,扫描用户设定的包下面所有的类
            doScanner(properties.getProperty("scanPackage"));

            //3.拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写
            doInstance();
            // 4.依赖注入，实现ioc机制
            doIoc();

            //5.初始化HandlerMapping(将url和method对应上)
            initHandlerMapping();
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void doIoc() {

        if (IOC.isEmpty()) {
            System.err.println(" doIoc 没有发现可注入的实例");
            return;
        }
        try {
            for (Map.Entry<String, Object> entry : IOC.entrySet()) {
                Field[] fileds = entry.getValue().getClass().getDeclaredFields();
                // 遍历bean对象的字段
                for (Field filed : fileds) {
                    if (filed.isAnnotationPresent(MyQualifier.class)) {
                        // 通过bean字段对象上面的注解参数来注入实例
                        String beanName = filed.getAnnotation(MyQualifier.class).value();
                        if ("".equals(beanName)) {
                            // 如果使用@MyController，@MyService没有配置value的值，默认使用类名 首字母小写
                            beanName = toLowerFirstLetter(filed.getType().getSimpleName());
                        }
                        filed.setAccessible(true);
                        filed.set(entry.getValue(), IOC.get(beanName));
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private void initHandlerMapping() {
        if (IOC.isEmpty()) {
            System.err.println("initHandlerMapping 没有发现可注入的实例");
            return;
        }

        for (Map.Entry<String, Object> entry : IOC.entrySet()) {
//            synchronized (lock) {

            Class<?> clazz = entry.getValue().getClass();

            if (!clazz.isAnnotationPresent(MyController.class)) {
                continue;
            }
            //拼url时,是controller头的url拼上方法上的url
            String baseUrl = "";
            if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                baseUrl = clazz.getAnnotation(MyRequestMapping.class).value();
            }
            Method[] methods = clazz.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                    continue;
                }
                MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                String url = annotation.value();
                //去除多个 /
                url = (baseUrl + "/" + url).replaceAll("/+", "/");
                handlerMapping.put(url, method);
                controllerMaps.put(url, entry.getValue());
                System.out.println("The " + url + ", Mapped: " + method);
            }
//            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()) {
            System.err.println("classNames is empty,请检查 是否配置 scanPackage");
            return;
        }

        try {
            for (String className : classNames) {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {

                    Object instance = clazz.newInstance();
                    MyController myController = clazz.getAnnotation(MyController.class);
                    String beanName = myController.value();
                    if ("".equals(beanName)) {
                        beanName = toLowerFirstLetter(clazz.getSimpleName());
                    }

                    IOC.put(beanName, instance);

                } else if (clazz.isAnnotationPresent(MyService.class)) {
                    Object instance = clazz.newInstance();
                    MyService myService = clazz.getAnnotation(MyService.class);
                    String beanName = myService.value();
                    if ("".equals(beanName)) {
                        beanName = toLowerFirstLetter(clazz.getSimpleName());
                    }

                    IOC.put(beanName, instance);


                } else {
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 把字符串的首字母小写
     *
     * @param name
     * @return
     */
    private String toLowerFirstLetter(String name) {
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }


    private void doScanner(String basePackageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + basePackageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                doScanner(basePackageName + "." + file.getName());
            } else {
                String className = basePackageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != resourceAsStream) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
