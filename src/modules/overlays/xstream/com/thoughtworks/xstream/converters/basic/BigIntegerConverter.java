package com.thoughtworks.xstream.converters.basic;

import java.math.BigInteger;

public class BigIntegerConverter extends AbstractSingleValueConverter {
   public boolean canConvert(Class type) {
      return type == BigInteger.class;
   }

   public Object fromString(String str) {
      return new BigInteger(str);
   }
}
