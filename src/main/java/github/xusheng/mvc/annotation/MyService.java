package github.xusheng.mvc.annotation;

import java.lang.annotation.*;

/**
 * @Description
 * @Author xusheng
 * @Create 2019-01-08 22:53
 * @Version 1.0
 **/
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyService {

    String value() default "";
}
