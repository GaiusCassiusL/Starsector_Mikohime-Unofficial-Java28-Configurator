package com.thoughtworks.xstream.converters.basic;

public class BooleanConverter extends AbstractSingleValueConverter {
   public static final BooleanConverter TRUE_FALSE = new BooleanConverter("true", "false", false);
   public static final BooleanConverter YES_NO = new BooleanConverter("yes", "no", false);
   public static final BooleanConverter BINARY = new BooleanConverter("1", "0", true);
   private final String positive;
   private final String negative;
   private final boolean caseSensitive;

   public BooleanConverter(String positive, String negative, boolean caseSensitive) {
      this.positive = positive;
      this.negative = negative;
      this.caseSensitive = caseSensitive;
   }

   public BooleanConverter() {
      this("true", "false", false);
   }

   /** @deprecated */
   public boolean shouldConvert(Class type, Object value) {
      return true;
   }

   public boolean canConvert(Class type) {
      return type == boolean.class || type == Boolean.class;
   }

   public Object fromString(String str) {
      if (this.caseSensitive) {
         return this.positive.equals(str) ? Boolean.TRUE : Boolean.FALSE;
      } else {
         return this.positive.equalsIgnoreCase(str) ? Boolean.TRUE : Boolean.FALSE;
      }
   }

   public String toString(Object obj) {
      Boolean value = (Boolean)obj;
      return obj == null ? null : (value ? this.positive : this.negative);
   }
}
