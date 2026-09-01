package com.thoughtworks.xstream.converters;

public interface MarshallingContext extends DataHolder {
   void convertAnother(Object var1);

   void convertAnother(Object var1, Converter var2);
}
