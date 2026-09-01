package com.thoughtworks.xstream.converters;

public interface SingleValueConverter extends ConverterMatcher {
   String toString(Object var1);

   Object fromString(String var1);
}
