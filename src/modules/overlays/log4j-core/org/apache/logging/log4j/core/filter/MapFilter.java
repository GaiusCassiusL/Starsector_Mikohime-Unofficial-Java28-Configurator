package org.apache.logging.log4j.core.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.IndexedStringMap;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.SortedArrayStringMap;

@Configurable(elementType = "filter", printObject = true)
@Plugin
@PerformanceSensitive("allocation")
public class MapFilter extends AbstractFilter {
   private final IndexedStringMap map;
   private final boolean isAnd;

   protected MapFilter(final Map<String, List<String>> map, final boolean oper, final Filter.Result onMatch, final Filter.Result onMismatch) {
      super(onMatch, onMismatch);
      this.isAnd = oper;
      Objects.requireNonNull(map, "map cannot be null");
      this.map = new SortedArrayStringMap(map.size());

      for (Entry<String, List<String>> entry : map.entrySet()) {
         this.map.putValue(entry.getKey(), entry.getValue());
      }
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
      if (msg instanceof MapMessage) {
         return this.filter((MapMessage<?, ?>)msg) ? this.onMatch : this.onMismatch;
      } else {
         return Filter.Result.NEUTRAL;
      }
   }

   @Override
   public Filter.Result filter(final LogEvent event) {
      Message msg = event.getMessage();
      if (msg instanceof MapMessage) {
         return this.filter((MapMessage<?, ?>)msg) ? this.onMatch : this.onMismatch;
      } else {
         return Filter.Result.NEUTRAL;
      }
   }

   protected boolean filter(final MapMessage<?, ?> mapMessage) {
      boolean match = false;

      for (int i = 0; i < this.map.size(); i++) {
         String toMatch = mapMessage.get(this.map.getKeyAt(i));
         match = toMatch != null && ((List)this.map.getValueAt(i)).contains(toMatch);
         if (!this.isAnd && match || this.isAnd && !match) {
            break;
         }
      }

      return match;
   }

   protected boolean filter(final Map<String, String> data) {
      boolean match = false;

      for (int i = 0; i < this.map.size(); i++) {
         String toMatch = data.get(this.map.getKeyAt(i));
         match = toMatch != null && ((List)this.map.getValueAt(i)).contains(toMatch);
         if (!this.isAnd && match || this.isAnd && !match) {
            break;
         }
      }

      return match;
   }

   protected boolean filter(final ReadOnlyStringMap data) {
      boolean match = false;

      for (int i = 0; i < this.map.size(); i++) {
         String toMatch = (String)data.getValue(this.map.getKeyAt(i));
         match = toMatch != null && ((List)this.map.getValueAt(i)).contains(toMatch);
         if (!this.isAnd && match || this.isAnd && !match) {
            break;
         }
      }

      return match;
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

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("isAnd=").append(this.isAnd);
      if (this.map.size() > 0) {
         sb.append(", {");

         for (int i = 0; i < this.map.size(); i++) {
            if (i > 0) {
               sb.append(", ");
            }

            List<String> list = (List<String>)this.map.getValueAt(i);
            String value = list.size() > 1 ? list.get(0) : list.toString();
            sb.append(this.map.getKeyAt(i)).append('=').append(value);
         }

         sb.append('}');
      }

      return sb.toString();
   }

   protected boolean isAnd() {
      return this.isAnd;
   }

   protected IndexedReadOnlyStringMap getStringMap() {
      return this.map;
   }

   @PluginFactory
   public static MapFilter createFilter(
      @PluginElement final KeyValuePair[] pairs,
      @PluginAttribute final String operator,
      @PluginAttribute final Filter.Result onMatch,
      @PluginAttribute final Filter.Result onMismatch
   ) {
      if (pairs != null && pairs.length != 0) {
         Map<String, List<String>> map = new HashMap<>();

         for (KeyValuePair pair : pairs) {
            String key = pair.getKey();
            if (key == null) {
               LOGGER.error("A null key is not valid in MapFilter");
            } else {
               String value = pair.getValue();
               if (value == null) {
                  LOGGER.error("A null value for key " + key + " is not allowed in MapFilter");
               } else {
                  List<String> list = map.get(pair.getKey());
                  if (list != null) {
                     list.add(value);
                  } else {
                     list = new ArrayList<>();
                     list.add(value);
                     map.put(pair.getKey(), list);
                  }
               }
            }
         }

         if (map.isEmpty()) {
            LOGGER.error("MapFilter is not configured with any valid key value pairs");
            return null;
         } else {
            boolean isAnd = operator == null || !operator.equalsIgnoreCase("or");
            return new MapFilter(map, isAnd, onMatch, onMismatch);
         }
      } else {
         LOGGER.error("keys and values must be specified for the MapFilter");
         return null;
      }
   }
}
