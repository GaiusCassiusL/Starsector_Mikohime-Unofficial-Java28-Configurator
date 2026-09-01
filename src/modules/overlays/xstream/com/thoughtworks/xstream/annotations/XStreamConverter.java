package com.thoughtworks.xstream.annotations;

import com.thoughtworks.xstream.converters.ConverterMatcher;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
@Documented
public @interface XStreamConverter {
   Class<? extends ConverterMatcher> value();

   int priority() default 0;

   boolean useImplicitType() default true;

   Class<?>[] types() default {};

   String[] strings() default {};

   byte[] bytes() default {};

   char[] chars() default {};

   short[] shorts() default {};

   int[] ints() default {};

   long[] longs() default {};

   float[] floats() default {};

   double[] doubles() default {};

   boolean[] booleans() default {};

   Class<?>[] nulls() default {};
}
