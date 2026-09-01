package org.apache.logging.log4j.core.appender.nosql;

import java.io.Closeable;

public interface NoSqlConnection<W, T extends NoSqlObject<W>> extends Closeable {
   T createObject();

   T[] createList(int length);

   void insertObject(NoSqlObject<W> object);

   @Override
   void close();

   boolean isClosed();
}
