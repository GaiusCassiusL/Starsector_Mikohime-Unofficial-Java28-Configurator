package com.thoughtworks.xstream.annotations;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

@Deprecated
public class AnnotationProvider {
   @Deprecated
   public <T extends Annotation> T getAnnotation(Field field, Class<T> annotationClass) {
      return field.getAnnotation(annotationClass);
   }
}
