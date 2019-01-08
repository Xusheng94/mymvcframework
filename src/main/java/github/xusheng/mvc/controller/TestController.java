package github.xusheng.mvc.controller;

import github.xusheng.mvc.annotation.MyController;
import github.xusheng.mvc.annotation.MyQualifier;
import github.xusheng.mvc.annotation.MyRequestMapping;
import github.xusheng.mvc.annotation.MyRequestParam;
import github.xusheng.mvc.service.TestService;
import github.xusheng.mvc.service.TestService2;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @Description
 * @Author xusheng
 * @Create 2019-01-09 00:06
 * @Version 1.0
 **/
@MyController
@MyRequestMapping("/test")
public class TestController {

    @MyQualifier("testService")
    private TestService testService;
    // 测试@DhService使用默认值
    @MyQualifier("testService2Impl")
    private TestService2 testService2;

    /**
     * 测试的url：http://localhost:8080/test/1?str_param=233&int_param=2&float_param=1.2&double_param=2.5
     * @param request
     * @param response
     * @param strParam
     * @param intParam
     * @param floatParam
     * @param doubleParam
     */
    @MyRequestMapping("/1")
    public void test1(HttpServletRequest request, HttpServletResponse response,
                      @MyRequestParam("str_param") String strParam,
                      @MyRequestParam("int_param") Integer intParam,
                      @MyRequestParam("float_param") Float floatParam,
                      @MyRequestParam("double_param") Double doubleParam) {
        testService.doServiceTest();
        testService2.doServiceTest();
        try {
            response.getWriter().write(
                    "String parameter: " + strParam +
                            "\nInteger parameter: " + intParam +
                            "\nFloat parameter: " + floatParam +
                            "\nDouble parameter: " + doubleParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
