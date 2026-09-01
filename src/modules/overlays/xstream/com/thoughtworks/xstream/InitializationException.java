package com.thoughtworks.xstream;

public class InitializationException extends XStream.InitializationException {
   public InitializationException(String message, Throwable cause) {
      super(message, cause);
   }

   public InitializationException(String message) {
      super(message);
   }
}
