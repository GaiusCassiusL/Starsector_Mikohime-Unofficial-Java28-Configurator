package org.apache.logging.log4j.core.appender.nosql;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultNoSqlObject implements NoSqlObject<Map<String, Object>> {
   private final Map<String, Object> map = new HashMap<>();

   @Override
   public void set(final String field, final Object value) {
      this.map.put(field, value);
   }

   @Override
   public void set(final String field, final NoSqlObject<Map<String, Object>> value) {
      this.map.put(field, value.unwrap());
   }

   @Override
   public void set(final String field, final Object[] values) {
      this.map.put(field, Arrays.asList(values));
   }

   @Override
   public void set(final String field, final NoSqlObject<Map<String, Object>>[] values) {
      List<Map<String, Object>> list = new ArrayList<>(values.length);

      for (NoSqlObject<Map<String, Object>> value : values) {
         list.add(value.unwrap());
      }

      this.map.put(field, list);
   }

   public Map<String, Object> unwrap() {
      return this.map;
   }
}
