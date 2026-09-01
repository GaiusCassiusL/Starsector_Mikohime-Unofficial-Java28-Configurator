package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LogEvent;

public interface Filterable extends LifeCycle {
   void addFilter(Filter filter);

   void removeFilter(Filter filter);

   Filter getFilter();

   boolean hasFilter();

   boolean isFiltered(LogEvent event);
}
