package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Configurable(elementType = "filter", printObject = true)
@Plugin
@PerformanceSensitive("allocation")
public final class LevelRangeFilter extends AbstractFilter {
   public static final Level DEFAULT_MIN_LEVEL = Level.OFF;
   public static final Level DEFAULT_MAX_LEVEL = Level.ALL;
   public static final Filter.Result DEFAULT_ON_MATCH = Filter.Result.NEUTRAL;
   public static final Filter.Result DEFAULT_ON_MISMATCH = Filter.Result.DENY;
   private final Level maxLevel;
   private final Level minLevel;

   @PluginFactory
   public static LevelRangeFilter createFilter(
      @PluginAttribute final Level minLevel,
      @PluginAttribute final Level maxLevel,
      @PluginAttribute final Filter.Result onMatch,
      @PluginAttribute final Filter.Result onMismatch
   ) {
      Level effectiveMinLevel = minLevel == null ? DEFAULT_MIN_LEVEL : minLevel;
      Level effectiveMaxLevel = maxLevel == null ? DEFAULT_MAX_LEVEL : maxLevel;
      Filter.Result effectiveOnMatch = onMatch == null ? DEFAULT_ON_MATCH : onMatch;
      Filter.Result effectiveOnMismatch = onMismatch == null ? DEFAULT_ON_MISMATCH : onMismatch;
      return new LevelRangeFilter(effectiveMinLevel, effectiveMaxLevel, effectiveOnMatch, effectiveOnMismatch);
   }

   private LevelRangeFilter(final Level minLevel, final Level maxLevel, final Filter.Result onMatch, final Filter.Result onMismatch) {
      super(onMatch, onMismatch);
      this.minLevel = minLevel;
      this.maxLevel = maxLevel;
   }

   private Filter.Result filter(final Level level) {
      return level.isInRange(this.minLevel, this.maxLevel) ? this.onMatch : this.onMismatch;
   }

   @Override
   public Filter.Result filter(final LogEvent event) {
      return this.filter(event.getLevel());
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2) {
      return this.filter(level);
   }

   @Override
   public Filter.Result filter(
      final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2, final Object p3
   ) {
      return this.filter(level);
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
      return this.filter(level);
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
      return this.filter(level);
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
      return this.filter(level);
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
      return this.filter(level);
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
      return this.filter(level);
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
      return this.filter(level);
   }

   public Level getMinLevel() {
      return this.minLevel;
   }

   public Level getMaxLevel() {
      return this.maxLevel;
   }

   @Override
   public String toString() {
      return String.format("[%s,%s]", this.minLevel, this.maxLevel);
   }
}
