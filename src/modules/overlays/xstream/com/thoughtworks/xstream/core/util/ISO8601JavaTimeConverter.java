package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.basic.AbstractSingleValueConverter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.WeekFields;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

public class ISO8601JavaTimeConverter extends AbstractSingleValueConverter {
   private static final DateTimeFormatter STD_DATE_TIME = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
      .appendFraction(ChronoField.NANO_OF_SECOND, 3, 9, true)
      .appendOffsetId()
      .toFormatter();
   private static final DateTimeFormatter STD_ORDINAL_DATE_TIME = new DateTimeFormatterBuilder()
      .appendPattern("yyyy-DDD'T'HH:mm:ss")
      .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
      .appendOffsetId()
      .toFormatter();
   private static final DateTimeFormatter BASIC_DATE_TIME = new DateTimeFormatterBuilder()
      .appendPattern("yyyyMMdd'T'HHmmss")
      .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
      .appendOffsetId()
      .toFormatter();
   private static final DateTimeFormatter BASIC_ORDINAL_DATE_TIME = new DateTimeFormatterBuilder()
      .appendPattern("yyyyDDD'T'HHmmss")
      .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
      .appendOffsetId()
      .toFormatter();
   private static final DateTimeFormatter BASIC_TIME = new DateTimeFormatterBuilder()
      .appendPattern("HHmmss")
      .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
      .appendOffsetId()
      .toFormatter();
   private static final DateTimeFormatter ISO_TTIME = new DateTimeFormatterBuilder()
      .appendPattern("'T'HH:mm:ss")
      .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
      .appendOffsetId()
      .toFormatter();
   private static final DateTimeFormatter BASIC_TTIME = new DateTimeFormatterBuilder()
      .appendPattern("'T'HHmmss")
      .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
      .appendOffsetId()
      .toFormatter();
   private static final DateTimeFormatter ISO_WEEK_DATE_TIME = new DateTimeFormatterBuilder()
      .appendPattern("YYYY-'W'ww-e'T'HH:mm:ss")
      .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
      .appendOffsetId()
      .toFormatter();
   private static final DateTimeFormatter BASIC_WEEK_DATE_TIME = new DateTimeFormatterBuilder()
      .appendPattern("YYYY'W'wwe'T'HHmmss")
      .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
      .appendOffsetId()
      .toFormatter();
   private static final DateTimeFormatter BASIC_ORDINAL_DATE = new DateTimeFormatterBuilder().appendPattern("yyyyDDD").toFormatter();
   private static final DateTimeFormatter BASIC_WEEK_DATE = new DateTimeFormatterBuilder().appendPattern("YYYY'W'wwe").toFormatter();
   private static final DateTimeFormatter STD_DATE_HOUR = new DateTimeFormatterBuilder().appendPattern("yyyy-MM-dd'T'HH").toFormatter();
   private static final DateTimeFormatter STD_HOUR = new DateTimeFormatterBuilder().appendPattern("HH").toFormatter();
   private static final DateTimeFormatter STD_YEAR_WEEK = new DateTimeFormatterBuilder()
      .appendPattern("YYYY-'W'ww")
      .parseDefaulting(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR, 1L)
      .toFormatter();

   @Override
   public boolean canConvert(Class type) {
      return false;
   }

   @Override
   public Object fromString(String str) {
      try {
         OffsetDateTime odt = OffsetDateTime.parse(str);
         return GregorianCalendar.from(odt.atZoneSameInstant(ZoneId.systemDefault()));
      } catch (DateTimeParseException var29) {
         try {
            LocalDateTime ldt = LocalDateTime.parse(str);
            return GregorianCalendar.from(ldt.atZone(ZoneId.systemDefault()));
         } catch (DateTimeParseException var28) {
            try {
               Instant instant = Instant.parse(str);
               return GregorianCalendar.from(instant.atZone(ZoneId.systemDefault()));
            } catch (DateTimeParseException var27) {
               try {
                  OffsetDateTime odt = BASIC_DATE_TIME.parse(str, OffsetDateTime::from);
                  return GregorianCalendar.from(odt.atZoneSameInstant(ZoneId.systemDefault()));
               } catch (DateTimeParseException var26) {
                  try {
                     OffsetDateTime odt = STD_ORDINAL_DATE_TIME.parse(str, OffsetDateTime::from);
                     return GregorianCalendar.from(odt.atZoneSameInstant(ZoneId.systemDefault()));
                  } catch (DateTimeParseException var25) {
                     try {
                        OffsetDateTime odt = BASIC_ORDINAL_DATE_TIME.parse(str, OffsetDateTime::from);
                        return GregorianCalendar.from(odt.atZoneSameInstant(ZoneId.systemDefault()));
                     } catch (DateTimeParseException var24) {
                        try {
                           OffsetTime ot = OffsetTime.parse(str);
                           return GregorianCalendar.from(ot.atDate(LocalDate.ofEpochDay(0L)).atZoneSameInstant(ZoneId.systemDefault()));
                        } catch (DateTimeParseException var23) {
                           try {
                              OffsetTime ot = BASIC_TIME.parse(str, OffsetTime::from);
                              return GregorianCalendar.from(ot.atDate(LocalDate.ofEpochDay(0L)).atZoneSameInstant(ZoneId.systemDefault()));
                           } catch (DateTimeParseException var22) {
                              try {
                                 OffsetTime ot = ISO_TTIME.parse(str, OffsetTime::from);
                                 return GregorianCalendar.from(ot.atDate(LocalDate.ofEpochDay(0L)).atZoneSameInstant(ZoneId.systemDefault()));
                              } catch (DateTimeParseException var21) {
                                 try {
                                    OffsetTime ot = BASIC_TTIME.parse(str, OffsetTime::from);
                                    return GregorianCalendar.from(ot.atDate(LocalDate.ofEpochDay(0L)).atZoneSameInstant(ZoneId.systemDefault()));
                                 } catch (DateTimeParseException var20) {
                                    try {
                                       TemporalAccessor ta = ISO_WEEK_DATE_TIME.withLocale(Locale.getDefault()).parse(str);
                                       Year y = Year.from(ta);
                                       MonthDay md = MonthDay.from(ta);
                                       OffsetTime ot = OffsetTime.from(ta);
                                       return GregorianCalendar.from(ot.atDate(y.atMonthDay(md)).atZoneSameInstant(ZoneId.systemDefault()));
                                    } catch (DateTimeParseException var19) {
                                       try {
                                          TemporalAccessor ta = BASIC_WEEK_DATE_TIME.withLocale(Locale.getDefault()).parse(str);
                                          Year y = Year.from(ta);
                                          MonthDay md = MonthDay.from(ta);
                                          OffsetTime ot = OffsetTime.from(ta);
                                          return GregorianCalendar.from(ot.atDate(y.atMonthDay(md)).atZoneSameInstant(ZoneId.systemDefault()));
                                       } catch (DateTimeParseException var18) {
                                          try {
                                             LocalDate ld = LocalDate.parse(str);
                                             return GregorianCalendar.from(ld.atStartOfDay(ZoneId.systemDefault()));
                                          } catch (DateTimeParseException var17) {
                                             try {
                                                LocalDate ld = LocalDate.parse(str, DateTimeFormatter.BASIC_ISO_DATE);
                                                return GregorianCalendar.from(ld.atStartOfDay(ZoneId.systemDefault()));
                                             } catch (DateTimeParseException var16) {
                                                try {
                                                   LocalDate ld = LocalDate.parse(str, DateTimeFormatter.ISO_ORDINAL_DATE);
                                                   return GregorianCalendar.from(ld.atStartOfDay(ZoneId.systemDefault()));
                                                } catch (DateTimeParseException var15) {
                                                   try {
                                                      LocalDate ld = BASIC_ORDINAL_DATE.parse(str, LocalDate::from);
                                                      return GregorianCalendar.from(ld.atStartOfDay(ZoneId.systemDefault()));
                                                   } catch (DateTimeParseException var14) {
                                                      try {
                                                         LocalDate ld = LocalDate.parse(str, DateTimeFormatter.ISO_WEEK_DATE.withLocale(Locale.getDefault()));
                                                         return GregorianCalendar.from(ld.atStartOfDay(ZoneId.systemDefault()));
                                                      } catch (DateTimeParseException var13) {
                                                         try {
                                                            TemporalAccessor ta = BASIC_WEEK_DATE.withLocale(Locale.getDefault()).parse(str);
                                                            Year y = Year.from(ta);
                                                            MonthDay md = MonthDay.from(ta);
                                                            return GregorianCalendar.from(y.atMonthDay(md).atStartOfDay(ZoneId.systemDefault()));
                                                         } catch (DateTimeParseException var12) {
                                                            try {
                                                               LocalDateTime ldt = STD_DATE_HOUR.parse(str, LocalDateTime::from);
                                                               return GregorianCalendar.from(ldt.atZone(ZoneId.systemDefault()));
                                                            } catch (DateTimeParseException var11) {
                                                               try {
                                                                  LocalTime lt = STD_HOUR.parse(str, LocalTime::from);
                                                                  return GregorianCalendar.from(
                                                                     lt.atDate(LocalDate.ofEpochDay(0L)).atZone(ZoneId.systemDefault())
                                                                  );
                                                               } catch (DateTimeParseException var10) {
                                                                  try {
                                                                     LocalTime lt = LocalTime.parse(str);
                                                                     return GregorianCalendar.from(
                                                                        lt.atDate(LocalDate.ofEpochDay(0L)).atZone(ZoneId.systemDefault())
                                                                     );
                                                                  } catch (DateTimeParseException var9) {
                                                                     try {
                                                                        YearMonth ym = YearMonth.parse(str);
                                                                        return GregorianCalendar.from(ym.atDay(1).atStartOfDay(ZoneId.systemDefault()));
                                                                     } catch (DateTimeParseException var8) {
                                                                        try {
                                                                           Year y = Year.parse(str);
                                                                           return GregorianCalendar.from(y.atDay(1).atStartOfDay(ZoneId.systemDefault()));
                                                                        } catch (DateTimeParseException var7) {
                                                                           try {
                                                                              TemporalAccessor ta = STD_YEAR_WEEK.withLocale(Locale.getDefault()).parse(str);
                                                                              int y = ta.get(WeekFields.ISO.weekBasedYear());
                                                                              int w = ta.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                                                                              return GregorianCalendar.from(
                                                                                 LocalDateTime.from(ta)
                                                                                    .with(WeekFields.ISO.weekOfYear(), y)
                                                                                    .with(WeekFields.ISO.weekOfWeekBasedYear(), w)
                                                                                    .atZone(ZoneId.systemDefault())
                                                                              );
                                                                           } catch (DateTimeParseException var6) {
                                                                              ConversionException exception = new ConversionException("Cannot parse date");
                                                                              exception.add("date", str);
                                                                              throw exception;
                                                                           }
                                                                        }
                                                                     }
                                                                  }
                                                               }
                                                            }
                                                         }
                                                      }
                                                   }
                                                }
                                             }
                                          }
                                       }
                                    }
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }
      }
   }

   @Override
   public String toString(Object obj) {
      Calendar calendar = (Calendar)obj;
      Instant instant = Instant.ofEpochMilli(calendar.getTimeInMillis());
      int offsetInMillis = calendar.getTimeZone().getOffset(calendar.getTimeInMillis());
      OffsetDateTime offsetDateTime = OffsetDateTime.ofInstant(instant, ZoneOffset.ofTotalSeconds(offsetInMillis / 1000));
      return STD_DATE_TIME.format(offsetDateTime);
   }
}
