package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.nio.charset.Charset;

public class CharsetConverter extends AbstractSingleValueConverter {
   public boolean canConvert(Class type) {
      return type != null && Charset.class.isAssignableFrom(type);
   }

   public String toString(Object obj) {
      return obj == null ? null : ((Charset)obj).name();
   }

   public Object fromString(String str) {
      return Charset.forName(str);
   }
}
