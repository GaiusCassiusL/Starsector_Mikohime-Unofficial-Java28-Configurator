package org.apache.logging.log4j.core.lookup;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;

@Lookup
@Plugin("date")
public class DateLookup implements StrLookup {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final Marker LOOKUP = MarkerManager.getMarker("LOOKUP");

   @Override
   public String lookup(final String key) {
      return this.formatDate(System.currentTimeMillis(), key);
   }

   @Override
   public String lookup(final LogEvent event, final String key) {
      return event == null ? this.lookup(key) : this.formatDate(event.getTimeMillis(), key);
   }

   private String formatDate(final long date, final String format) {
      DateFormat dateFormat = null;
      if (format != null) {
         try {
            dateFormat = new SimpleDateFormat(format);
         } catch (Exception ex) {
            LOGGER.error(LOOKUP, "Invalid date format: [{}], using default", format, ex);
         }
      }

      if (dateFormat == null) {
         dateFormat = DateFormat.getInstance();
      }

      return dateFormat.format(new Date(date));
   }
}
