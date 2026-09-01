package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.sql.Date;

public class SqlDateConverter extends AbstractSingleValueConverter {
   public boolean canConvert(Class type) {
      return type == Date.class;
   }

   public Object fromString(String str) {
      return Date.valueOf(str);
   }
}
