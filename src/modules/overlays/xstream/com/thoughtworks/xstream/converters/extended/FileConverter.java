package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.io.File;

public class FileConverter extends AbstractSingleValueConverter {
   public boolean canConvert(Class type) {
      return type == File.class;
   }

   public Object fromString(String str) {
      return new File(str);
   }

   public String toString(Object obj) {
      return ((File)obj).getPath();
   }
}
