package com.thoughtworks.xstream.converters;

import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;
import java.util.Iterator;
import java.util.Map;

public abstract class ErrorWritingException extends XStreamException implements ErrorWriter {
   private static final String SEPARATOR = "\n-------------------------------";
   private final Map stuff = new OrderRetainingMap();

   public ErrorWritingException(String message) {
      super(message);
      this.addData(message, null);
   }

   public ErrorWritingException(Throwable cause) {
      super(cause);
      this.addData(null, cause);
   }

   public ErrorWritingException(String message, Throwable cause) {
      super(message, cause);
      this.addData(message, cause);
   }

   private void addData(String msg, Throwable cause) {
      if (msg != null) {
         this.add("message", msg);
      }

      if (cause != null) {
         this.add("cause-exception", cause.getClass().getName());
         this.add("cause-message", cause instanceof ErrorWritingException ? ((ErrorWritingException)cause).getShortMessage() : cause.getMessage());
      }
   }

   public String get(String errorKey) {
      return (String)this.stuff.get(errorKey);
   }

   public void add(String name, String information) {
      String key = name;

      for (int i = 0; this.stuff.containsKey(key); key = name + "[" + ++i + "]") {
         String value = (String)this.stuff.get(key);
         if (information.equals(value)) {
            return;
         }
      }

      this.stuff.put(key, information);
   }

   public void set(String name, String information) {
      String key = name;
      int i = 0;
      this.stuff.put(key, information);

      for (; this.stuff.containsKey(key); key = name + "[" + ++i + "]") {
         if (i != 0) {
            this.stuff.remove(key);
         }
      }
   }

   public Iterator keys() {
      return this.stuff.keySet().iterator();
   }

   public String getMessage() {
      StringBuffer result = new StringBuffer();
      if (super.getMessage() != null) {
         result.append(super.getMessage());
      }

      if (!result.toString().endsWith("\n-------------------------------")) {
         result.append("\n---- Debugging information ----");
      }

      Iterator iterator = this.keys();

      while (iterator.hasNext()) {
         String k = (String)iterator.next();
         String v = this.get(k);
         result.append('\n').append(k);
         result.append("                    ".substring(Math.min(20, k.length())));
         result.append(": ").append(v);
      }

      result.append("\n-------------------------------");
      return result.toString();
   }

   public String getShortMessage() {
      return super.getMessage();
   }
}
