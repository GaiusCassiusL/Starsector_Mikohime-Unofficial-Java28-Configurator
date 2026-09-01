package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.AbstractLifeCycle;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;

public abstract class AbstractFilter extends AbstractLifeCycle implements Filter {
   protected final Filter.Result onMatch;
   protected final Filter.Result onMismatch;

   protected AbstractFilter() {
      this(null, null);
   }

   protected AbstractFilter(final Filter.Result onMatch, final Filter.Result onMismatch) {
      this.onMatch = onMatch == null ? Filter.Result.NEUTRAL : onMatch;
      this.onMismatch = onMismatch == null ? Filter.Result.DENY : onMismatch;
   }

   @Override
   protected boolean equalsImpl(final Object obj) {
      if (this == obj) {
         return true;
      }

      if (!super.equalsImpl(obj)) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      AbstractFilter other = (AbstractFilter)obj;
      return this.onMatch != other.onMatch ? false : this.onMismatch == other.onMismatch;
   }

   @Override
   public Filter.Result filter(final LogEvent event) {
      return Filter.Result.NEUTRAL;
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
      return Filter.Result.NEUTRAL;
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
      return Filter.Result.NEUTRAL;
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
      return Filter.Result.NEUTRAL;
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
      return this.filter(logger, level, marker, msg, new Object[]{p0});
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1) {
      return this.filter(logger, level, marker, msg, p0, p1);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2) {
      return this.filter(logger, level, marker, msg, p0, p1, p2);
   }

   @Override
   public Filter.Result filter(
      final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2, final Object p3
   ) {
      return this.filter(logger, level, marker, msg, p0, p1, p2, p3);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4
   ) {
      return this.filter(logger, level, marker, msg, p0, p1, p2, p3, p4);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5
   ) {
      return this.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6
   ) {
      return this.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7
   ) {
      return this.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7,
      final Object p8
   ) {
      return this.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8);
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7,
      final Object p8,
      final Object p9
   ) {
      return this.filter(logger, level, marker, msg, p0, p1, p2, p3, p4, p5, p6, p7, p8, p9);
   }

   @Override
   public final Filter.Result getOnMatch() {
      return this.onMatch;
   }

   @Override
   public final Filter.Result getOnMismatch() {
      return this.onMismatch;
   }

   @Override
   protected int hashCodeImpl() {
      int prime = 31;
      int result = super.hashCodeImpl();
      result = 31 * result + (this.onMatch == null ? 0 : this.onMatch.hashCode());
      return 31 * result + (this.onMismatch == null ? 0 : this.onMismatch.hashCode());
   }

   @Override
   public String toString() {
      return this.getClass().getSimpleName();
   }

   public abstract static class AbstractFilterBuilder<B extends AbstractFilter.AbstractFilterBuilder<B>> {
      public static final String ATTR_ON_MISMATCH = "onMismatch";
      public static final String ATTR_ON_MATCH = "onMatch";
      @PluginBuilderAttribute("onMatch")
      private Filter.Result onMatch = Filter.Result.NEUTRAL;
      @PluginBuilderAttribute("onMismatch")
      private Filter.Result onMismatch = Filter.Result.DENY;

      public Filter.Result getOnMatch() {
         return this.onMatch;
      }

      public Filter.Result getOnMismatch() {
         return this.onMismatch;
      }

      public B setOnMatch(final Filter.Result onMatch) {
         this.onMatch = onMatch;
         return this.asBuilder();
      }

      public B setOnMismatch(final Filter.Result onMismatch) {
         this.onMismatch = onMismatch;
         return this.asBuilder();
      }

      public B asBuilder() {
         return (B)this;
      }
   }
}
