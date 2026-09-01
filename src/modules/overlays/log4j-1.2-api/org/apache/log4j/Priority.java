package org.apache.log4j;

public class Priority {
   public static final int OFF_INT = Integer.MAX_VALUE;
   public static final int FATAL_INT = 50000;
   public static final int ERROR_INT = 40000;
   public static final int WARN_INT = 30000;
   public static final int INFO_INT = 20000;
   public static final int DEBUG_INT = 10000;
   public static final int ALL_INT = Integer.MIN_VALUE;
   @Deprecated
   public static final Priority FATAL = new Level(50000, "FATAL", 0);
   @Deprecated
   public static final Priority ERROR = new Level(40000, "ERROR", 3);
   @Deprecated
   public static final Priority WARN = new Level(30000, "WARN", 4);
   @Deprecated
   public static final Priority INFO = new Level(20000, "INFO", 6);
   @Deprecated
   public static final Priority DEBUG = new Level(10000, "DEBUG", 7);
   transient int level;
   transient String levelStr;
   transient int syslogEquivalent;
   transient org.apache.logging.log4j.Level version2Level;

   protected Priority() {
      this.level = 10000;
      this.levelStr = "DEBUG";
      this.syslogEquivalent = 7;
   }

   protected Priority(final int level, final String levelStr, final int syslogEquivalent) {
      this.level = level;
      this.levelStr = levelStr;
      this.syslogEquivalent = syslogEquivalent;
   }

   @Override
   public boolean equals(final Object o) {
      if (o instanceof Priority) {
         Priority r = (Priority)o;
         return this.level == r.level;
      } else {
         return false;
      }
   }

   @Override
   public int hashCode() {
      return this.level;
   }

   public final int getSyslogEquivalent() {
      return this.syslogEquivalent;
   }

   public org.apache.logging.log4j.Level getVersion2Level() {
      return this.version2Level;
   }

   public boolean isGreaterOrEqual(final Priority r) {
      return this.level >= r.level;
   }

   @Deprecated
   public static Priority[] getAllPossiblePriorities() {
      return new Priority[]{FATAL, ERROR, Level.WARN, INFO, DEBUG};
   }

   @Override
   public final String toString() {
      return this.levelStr;
   }

   public final int toInt() {
      return this.level;
   }

   @Deprecated
   public static Priority toPriority(final String sArg) {
      return Level.toLevel(sArg);
   }

   @Deprecated
   public static Priority toPriority(final int val) {
      return toPriority(val, DEBUG);
   }

   @Deprecated
   public static Priority toPriority(final int val, final Priority defaultPriority) {
      return Level.toLevel(val, (Level)defaultPriority);
   }

   @Deprecated
   public static Priority toPriority(final String sArg, final Priority defaultPriority) {
      return Level.toLevel(sArg, (Level)defaultPriority);
   }
}
