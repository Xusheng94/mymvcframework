package github.xusheng.mvc.annotation;

import java.lang.annotation.*;

/**
 * @Description
 * @Author xusheng
 * @Create 2019-01-08 22:58
 * @Version 1.0
 **/
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyRequestParam {

    String value() default "";
}
