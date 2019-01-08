package github.xusheng.mvc.annotation;

import java.lang.annotation.*;

/**
 * @Description
 * @Author xusheng
 * @Create 2019-01-08 22:51
 * @Version 1.0
 **/
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MyQualifier {

    String value() default "";

}
