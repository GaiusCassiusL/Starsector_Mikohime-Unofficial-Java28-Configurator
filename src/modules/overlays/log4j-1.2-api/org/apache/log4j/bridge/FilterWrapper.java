package org.apache.log4j.bridge;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class FilterWrapper extends Filter {
   private final org.apache.logging.log4j.core.Filter filter;

   public static Filter adapt(final org.apache.logging.log4j.core.Filter filter) {
      if (filter instanceof Filter) {
         return (Filter)filter;
      } else if (filter instanceof FilterAdapter) {
         return ((FilterAdapter)filter).getFilter();
      } else {
         return filter != null ? new FilterWrapper(filter) : null;
      }
   }

   public FilterWrapper(final org.apache.logging.log4j.core.Filter filter) {
      this.filter = filter;
   }

   public org.apache.logging.log4j.core.Filter getFilter() {
      return this.filter;
   }

   @Override
   public int decide(final LoggingEvent event) {
      return 0;
   }
}
