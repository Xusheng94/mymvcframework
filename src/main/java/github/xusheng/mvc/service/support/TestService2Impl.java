package github.xusheng.mvc.service.support;

import github.xusheng.mvc.annotation.MyService;
import github.xusheng.mvc.service.TestService2;

/**
 * @Description
 * @Author xusheng
 * @Create 2019-01-09 00:13
 * @Version 1.0
 **/
@MyService
public class TestService2Impl implements TestService2 {

    @Override
    public void doServiceTest() {
        System.out.println("TestService2Impl 业务层执行方法了");

    }

}
