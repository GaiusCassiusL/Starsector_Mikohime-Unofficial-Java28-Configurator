package com.thoughtworks.xstream.converters.extended;

import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.util.Currency;

public class CurrencyConverter extends AbstractSingleValueConverter {
   public boolean canConvert(Class type) {
      return type == Currency.class;
   }

   public Object fromString(String str) {
      return Currency.getInstance(str);
   }
}
