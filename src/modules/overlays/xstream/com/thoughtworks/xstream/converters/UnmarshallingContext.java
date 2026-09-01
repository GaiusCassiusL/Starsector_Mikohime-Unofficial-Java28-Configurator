package com.thoughtworks.xstream.converters;

public interface UnmarshallingContext extends DataHolder {
   Object convertAnother(Object var1, Class var2);

   Object convertAnother(Object var1, Class var2, Converter var3);

   Object currentObject();

   Class getRequiredType();

   void addCompletionCallback(Runnable var1, int var2);
}
