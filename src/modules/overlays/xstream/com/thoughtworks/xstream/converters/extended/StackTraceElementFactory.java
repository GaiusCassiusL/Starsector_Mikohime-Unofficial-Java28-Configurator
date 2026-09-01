package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.core.util.Fields;
import java.lang.reflect.Field;

/** @deprecated */
public class StackTraceElementFactory {
   public StackTraceElement nativeMethodElement(String declaringClass, String methodName) {
      return this.create(declaringClass, methodName, "Native Method", -2);
   }

   public StackTraceElement unknownSourceElement(String declaringClass, String methodName) {
      return this.create(declaringClass, methodName, "Unknown Source", -1);
   }

   public StackTraceElement element(String declaringClass, String methodName, String fileName) {
      return this.create(declaringClass, methodName, fileName, -1);
   }

   public StackTraceElement element(String declaringClass, String methodName, String fileName, int lineNumber) {
      return this.create(declaringClass, methodName, fileName, lineNumber);
   }

   protected StackTraceElement create(String declaringClass, String methodName, String fileName, int lineNumber) {
      StackTraceElement result = new Throwable().getStackTrace()[0];
      this.setField(result, "declaringClass", declaringClass);
      this.setField(result, "methodName", methodName);
      this.setField(result, "fileName", fileName);
      this.setField(result, "lineNumber", new Integer(lineNumber));
      return result;
   }

   private void setField(StackTraceElement element, String fieldName, Object value) {
      Field field = Fields.find(StackTraceElement.class, fieldName);
      Fields.write(field, element, value);
   }
}
