package org.apache.logging.log4j.core.filter;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Supplier;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Configurable(elementType = "filter", printObject = true)
@Plugin
@PerformanceSensitive("allocation")
public final class TimeFilter extends AbstractFilter {
   private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
   private static final long HOUR_MS = 3600000L;
   private static final long DAY_MS = 86400000L;
   private volatile long start;
   private final LocalTime startTime;
   private volatile long end;
   private final LocalTime endTime;
   private final long duration;
   private final ZoneId timeZone;
   private final Clock clock;

   TimeFilter(
      final LocalTime start,
      final LocalTime end,
      final ZoneId timeZone,
      final Filter.Result onMatch,
      final Filter.Result onMismatch,
      final LocalDate now,
      final Clock clock
   ) {
      super(onMatch, onMismatch);
      this.startTime = start;
      this.endTime = end;
      this.timeZone = timeZone;
      this.start = ZonedDateTime.of(now, this.startTime, timeZone).withEarlierOffsetAtOverlap().toInstant().toEpochMilli();
      long endMillis = ZonedDateTime.of(now, this.endTime, timeZone).withEarlierOffsetAtOverlap().toInstant().toEpochMilli();
      if (end.isBefore(start)) {
         endMillis += 86400000L;
      }

      this.duration = this.startTime.isBefore(this.endTime)
         ? Duration.between(this.startTime, this.endTime).toMillis()
         : Duration.between(this.startTime, this.endTime).plusHours(24L).toMillis();
      long difference = endMillis - this.start - this.duration;
      if (difference != 0L) {
         endMillis -= difference;
      }

      this.end = endMillis;
      this.clock = clock;
   }

   private TimeFilter(
      final LocalTime start, final LocalTime end, final ZoneId timeZone, final Filter.Result onMatch, final Filter.Result onMismatch, final Clock clock
   ) {
      this(start, end, timeZone, onMatch, onMismatch, LocalDate.now(timeZone), clock);
   }

   private synchronized void adjustTimes(final long currentTimeMillis) {
      if (currentTimeMillis > this.end) {
         LocalDate date = Instant.ofEpochMilli(currentTimeMillis).atZone(this.timeZone).toLocalDate();
         this.start = ZonedDateTime.of(date, this.startTime, this.timeZone).withEarlierOffsetAtOverlap().toInstant().toEpochMilli();
         long endMillis = ZonedDateTime.of(date, this.endTime, this.timeZone).withEarlierOffsetAtOverlap().toInstant().toEpochMilli();
         if (this.endTime.isBefore(this.startTime)) {
            endMillis += 86400000L;
         }

         long difference = endMillis - this.start - this.duration;
         if (difference != 0L) {
            endMillis -= difference;
         }

         this.end = endMillis;
      }
   }

   Filter.Result filter(final long currentTimeMillis) {
      if (currentTimeMillis > this.end) {
         this.adjustTimes(currentTimeMillis);
      }

      return currentTimeMillis >= this.start && currentTimeMillis <= this.end ? this.onMatch : this.onMismatch;
   }

   @Override
   public Filter.Result filter(final LogEvent event) {
      return this.filter(event.getTimeMillis());
   }

   private Filter.Result filter() {
      return this.filter(this.clock.currentTimeMillis());
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
      return this.filter();
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
      return this.filter();
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
      return this.filter();
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
      return this.filter();
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1) {
      return this.filter();
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2) {
      return this.filter();
   }

   @Override
   public Filter.Result filter(
      final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2, final Object p3
   ) {
      return this.filter();
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
      return this.filter();
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
      return this.filter();
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
      return this.filter();
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
      return this.filter();
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
      return this.filter();
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
      return this.filter();
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("start=").append(this.start);
      sb.append(", end=").append(this.end);
      sb.append(", timezone=").append(this.timeZone.toString());
      return sb.toString();
   }

   @Deprecated(since = "3.0.0", forRemoval = true)
   public static TimeFilter createFilter(final String start, final String end, final String tz, final Filter.Result match, final Filter.Result mismatch) {
      TimeFilter.Builder builder = newBuilder().setStart(start).setEnd(end);
      if (tz != null) {
         builder.setTimezone(ZoneId.of(tz));
      }

      if (match != null) {
         builder.setOnMatch(match);
      }

      if (mismatch != null) {
         builder.setOnMismatch(mismatch);
      }

      return builder.get();
   }

   private static LocalTime parseTimestamp(final String timestamp, final LocalTime defaultValue) {
      if (timestamp == null) {
         return defaultValue;
      }

      try {
         return LocalTime.parse(timestamp, FORMATTER);
      } catch (Exception e) {
         LOGGER.warn("Error parsing TimeFilter timestamp value {}", timestamp, e);
         return defaultValue;
      }
   }

   @PluginFactory
   public static TimeFilter.Builder newBuilder() {
      return new TimeFilter.Builder();
   }

   public static class Builder extends AbstractFilter.AbstractFilterBuilder<TimeFilter.Builder> implements Supplier<TimeFilter> {
      private String start;
      private String end;
      @PluginAttribute
      private ZoneId timezone = ZoneId.systemDefault();
      private Clock clock;

      public TimeFilter.Builder setStart(@PluginAttribute final String start) {
         this.start = start;
         return this;
      }

      public TimeFilter.Builder setEnd(@PluginAttribute final String end) {
         this.end = end;
         return this;
      }

      public TimeFilter.Builder setTimezone(final ZoneId timezone) {
         this.timezone = timezone;
         return this;
      }

      @Inject
      public TimeFilter.Builder setClock(final Clock clock) {
         this.clock = clock;
         return this;
      }

      public TimeFilter get() {
         LocalTime startTime = TimeFilter.parseTimestamp(this.start, LocalTime.MIN);
         LocalTime endTime = TimeFilter.parseTimestamp(this.end, LocalTime.MAX);
         if (this.clock == null) {
            this.clock = ClockFactory.getClock();
         }

         return new TimeFilter(startTime, endTime, this.timezone, this.getOnMatch(), this.getOnMismatch(), this.clock);
      }
   }
}
