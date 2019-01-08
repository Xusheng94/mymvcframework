package github.xusheng.mvc.service.support;

import github.xusheng.mvc.annotation.MyService;
import github.xusheng.mvc.service.TestService;

/**
 * @Description
 * @Author xusheng
 * @Create 2019-01-09 00:11
 * @Version 1.0
 **/
@MyService("testService")
public class TestServiceImpl implements TestService {
    @Override
    public void doServiceTest() {
        System.out.println("TestServiceImpl 业务层执行方法了");
    }
}
