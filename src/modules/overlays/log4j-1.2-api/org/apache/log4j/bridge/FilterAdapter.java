package org.apache.log4j.bridge;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.core.filter.CompositeFilter;

public final class FilterAdapter extends AbstractFilter {
   private final Filter filter;

   public static org.apache.logging.log4j.core.Filter adapt(final Filter filter) {
      if (filter instanceof org.apache.logging.log4j.core.Filter) {
         return (org.apache.logging.log4j.core.Filter)filter;
      } else if (filter instanceof FilterWrapper && filter.getNext() == null) {
         return ((FilterWrapper)filter).getFilter();
      } else {
         return filter != null ? new FilterAdapter(filter) : null;
      }
   }

   public static Filter addFilter(final Filter first, final Filter second) {
      if (first == null) {
         return second;
      }

      if (second == null) {
         return first;
      }

      CompositeFilter composite;
      if (first instanceof FilterWrapper && ((FilterWrapper)first).getFilter() instanceof CompositeFilter) {
         composite = (CompositeFilter)((FilterWrapper)first).getFilter();
      } else {
         composite = CompositeFilter.createFilters(new org.apache.logging.log4j.core.Filter[]{adapt(first)});
      }

      return FilterWrapper.adapt(composite.addFilter(adapt(second)));
   }

   private FilterAdapter(final Filter filter) {
      this.filter = filter;
   }

   public Result filter(final LogEvent event) {
      LoggingEvent loggingEvent = new LogEventAdapter(event);

      for (Filter next = this.filter; next != null; next = next.getNext()) {
         switch (next.decide(loggingEvent)) {
            case -1:
               return Result.DENY;
            case 1:
               return Result.ACCEPT;
         }
      }

      return Result.NEUTRAL;
   }

   public Filter getFilter() {
      return this.filter;
   }

   public void start() {
      this.filter.activateOptions();
   }
}
