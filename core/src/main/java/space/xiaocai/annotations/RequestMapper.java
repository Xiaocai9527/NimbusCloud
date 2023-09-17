package space.xiaocai.annotations;

import space.xiaocai.api.Enums.RequestMethod;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestMapper {
    String value();
    RequestMethod method() default RequestMethod.GET;
}
