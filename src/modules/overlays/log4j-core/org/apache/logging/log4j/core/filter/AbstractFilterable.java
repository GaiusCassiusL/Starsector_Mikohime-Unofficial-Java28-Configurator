package org.apache.logging.log4j.core.filter;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.plugins.PluginElement;

public abstract class AbstractFilterable extends AbstractLifeCycle implements Filterable {
   private volatile Filter filter;
   private final Property[] propertyArray;

   protected AbstractFilterable() {
      this(null, null);
   }

   protected AbstractFilterable(final Filter filter, final Property[] propertyArray) {
      this.filter = filter;
      this.propertyArray = propertyArray == null ? Property.EMPTY_ARRAY : propertyArray;
   }

   @Override
   public synchronized void addFilter(final Filter filter) {
      if (filter != null) {
         if (this.filter == null) {
            this.filter = filter;
         } else if (this.filter instanceof CompositeFilter) {
            this.filter = ((CompositeFilter)this.filter).addFilter(filter);
         } else {
            Filter[] filters = new Filter[]{this.filter, filter};
            this.filter = CompositeFilter.createFilters(filters);
         }
      }
   }

   @Override
   public Filter getFilter() {
      return this.filter;
   }

   public Property[] getPropertyArray() {
      return this.propertyArray;
   }

   @Override
   public boolean hasFilter() {
      return this.filter != null;
   }

   @Override
   public boolean isFiltered(final LogEvent event) {
      return this.filter != null && this.filter.filter(event) == Filter.Result.DENY;
   }

   @Override
   public synchronized void removeFilter(final Filter filter) {
      if (this.filter != null && filter != null) {
         if (this.filter == filter || this.filter.equals(filter)) {
            this.filter = null;
         } else if (this.filter instanceof CompositeFilter) {
            CompositeFilter composite = (CompositeFilter)this.filter;
            composite = composite.removeFilter(filter);
            if (composite.size() > 1) {
               this.filter = composite;
            } else if (composite.size() == 1) {
               Iterator<Filter> iter = composite.iterator();
               this.filter = iter.next();
            } else {
               this.filter = null;
            }
         }
      }
   }

   @Override
   public void start() {
      this.setStarting();
      if (this.filter != null) {
         this.filter.start();
      }

      this.setStarted();
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      return this.stop(timeout, timeUnit, true);
   }

   protected boolean stop(final long timeout, final TimeUnit timeUnit, final boolean changeLifeCycleState) {
      if (changeLifeCycleState) {
         this.setStopping();
      }

      boolean stopped = true;
      if (this.filter != null) {
         stopped = this.filter.stop(timeout, timeUnit);
      }

      if (changeLifeCycleState) {
         this.setStopped();
      }

      return stopped;
   }

   public abstract static class Builder<B extends AbstractFilterable.Builder<B>> {
      @PluginElement("Filter")
      private Filter filter;
      @PluginElement("Properties")
      private Property[] propertyArray;

      public B asBuilder() {
         return (B)this;
      }

      public Filter getFilter() {
         return this.filter;
      }

      public Property[] getPropertyArray() {
         return this.propertyArray;
      }

      public B setFilter(final Filter filter) {
         this.filter = filter;
         return this.asBuilder();
      }

      public B setPropertyArray(final Property[] properties) {
         this.propertyArray = properties;
         return this.asBuilder();
      }
   }
}
