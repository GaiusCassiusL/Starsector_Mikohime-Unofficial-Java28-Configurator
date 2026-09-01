package org.apache.logging.log4j.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.util.Constants;
import org.apache.logging.log4j.util.EnglishEnums;

public interface Filter extends LifeCycle {
   Filter[] EMPTY_ARRAY = new Filter[0];
   String ELEMENT_TYPE = "filter";

   Filter.Result getOnMismatch();

   Filter.Result getOnMatch();

   Filter.Result filter(Logger logger, Level level, Marker marker, String msg, Object... params);

   Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object p0);

   Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1);

   Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2);

   Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3);

   Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4);

   Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5);

   Filter.Result filter(Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6);

   Filter.Result filter(
      Logger logger, Level level, Marker marker, String message, Object p0, Object p1, Object p2, Object p3, Object p4, Object p5, Object p6, Object p7
   );

   Filter.Result filter(
      Logger logger,
      Level level,
      Marker marker,
      String message,
      Object p0,
      Object p1,
      Object p2,
      Object p3,
      Object p4,
      Object p5,
      Object p6,
      Object p7,
      Object p8
   );

   Filter.Result filter(
      Logger logger,
      Level level,
      Marker marker,
      String message,
      Object p0,
      Object p1,
      Object p2,
      Object p3,
      Object p4,
      Object p5,
      Object p6,
      Object p7,
      Object p8,
      Object p9
   );

   Filter.Result filter(Logger logger, Level level, Marker marker, Object msg, Throwable t);

   Filter.Result filter(Logger logger, Level level, Marker marker, Message msg, Throwable t);

   default Filter.Result filter(Logger logger, Level level, Marker marker, String msg) {
      return this.filter(logger, level, marker, msg, Constants.EMPTY_OBJECT_ARRAY);
   }

   Filter.Result filter(LogEvent event);

   enum Result {
      ACCEPT,
      NEUTRAL,
      DENY;

      public static Filter.Result toResult(final String name) {
         return toResult(name, null);
      }

      public static Filter.Result toResult(final String name, final Filter.Result defaultResult) {
         return (Filter.Result)EnglishEnums.valueOf(Filter.Result.class, name, defaultResult);
      }
   }
}
