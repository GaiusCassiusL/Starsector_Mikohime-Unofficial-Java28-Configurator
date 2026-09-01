package org.apache.logging.log4j.core.filter;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

public class NeutralFilter extends AbstractFilter {
   public static final NeutralFilter INSTANCE = new NeutralFilter();

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
      return Filter.Result.NEUTRAL;
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1) {
      return Filter.Result.NEUTRAL;
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2) {
      return Filter.Result.NEUTRAL;
   }

   @Override
   public Filter.Result filter(
      final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2, final Object p3
   ) {
      return Filter.Result.NEUTRAL;
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
      return Filter.Result.NEUTRAL;
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
      return Filter.Result.NEUTRAL;
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
      return Filter.Result.NEUTRAL;
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
      return Filter.Result.NEUTRAL;
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
      return Filter.Result.NEUTRAL;
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
      return Filter.Result.NEUTRAL;
   }
}
