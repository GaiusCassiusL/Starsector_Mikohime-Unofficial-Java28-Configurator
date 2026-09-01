package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.ErrorWritingException;

public class ObjectAccessException extends ErrorWritingException {
   public ObjectAccessException(String message) {
      super(message);
   }

   public ObjectAccessException(String message, Throwable cause) {
      super(message, cause);
   }
}
