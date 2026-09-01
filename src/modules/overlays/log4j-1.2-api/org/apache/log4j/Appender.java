package org.apache.log4j;

import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public interface Appender {
   void addFilter(Filter newFilter);

   Filter getFilter();

   void clearFilters();

   void close();

   void doAppend(LoggingEvent event);

   String getName();

   void setErrorHandler(ErrorHandler errorHandler);

   ErrorHandler getErrorHandler();

   void setLayout(Layout layout);

   Layout getLayout();

   void setName(String name);

   boolean requiresLayout();
}
