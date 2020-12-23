package cn.yzstu.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Builder pattern annotation
 * @author Administrator
 */
// Acting on setter methods
@Target({ElementType.METHOD})
// Acting on compilation
@Retention(RetentionPolicy.SOURCE)
public @interface Builder {

}