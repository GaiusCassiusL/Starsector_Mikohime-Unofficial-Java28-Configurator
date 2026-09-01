package org.apache.logging.log4j.core.appender.nosql;

public interface NoSqlObject<W> {
   void set(String field, Object value);

   void set(String field, NoSqlObject<W> value);

   void set(String field, Object[] values);

   void set(String field, NoSqlObject<W>[] values);

   W unwrap();
}
