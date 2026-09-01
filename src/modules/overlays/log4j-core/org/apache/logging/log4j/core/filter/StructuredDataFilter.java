package org.apache.logging.log4j.core.filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.StructuredDataMessage;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.IndexedReadOnlyStringMap;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.StringBuilders;

@Configurable(elementType = "filter", printObject = true)
@Plugin
@PerformanceSensitive("allocation")
public final class StructuredDataFilter extends MapFilter {
   private static final int MAX_BUFFER_SIZE = 2048;
   private static final ThreadLocal<StringBuilder> threadLocalStringBuilder = new ThreadLocal<>();

   private StructuredDataFilter(final Map<String, List<String>> map, final boolean oper, final Filter.Result onMatch, final Filter.Result onMismatch) {
      super(map, oper, onMatch, onMismatch);
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
      return msg instanceof StructuredDataMessage ? this.filter((StructuredDataMessage)msg) : Filter.Result.NEUTRAL;
   }

   @Override
   public Filter.Result filter(final LogEvent event) {
      Message msg = event.getMessage();
      return msg instanceof StructuredDataMessage ? this.filter((StructuredDataMessage)msg) : super.filter(event);
   }

   protected Filter.Result filter(final StructuredDataMessage message) {
      boolean match = false;
      IndexedReadOnlyStringMap map = this.getStringMap();

      for (int i = 0; i < map.size(); i++) {
         StringBuilder toMatch = this.getValue(message, map.getKeyAt(i));
         if (toMatch != null) {
            match = this.listContainsValue((List<String>)map.getValueAt(i), toMatch);
         } else {
            match = false;
         }

         if (!this.isAnd() && match || this.isAnd() && !match) {
            break;
         }
      }

      return match ? this.onMatch : this.onMismatch;
   }

   private StringBuilder getValue(final StructuredDataMessage data, final String key) {
      StringBuilder sb = this.getStringBuilder();
      if (key.equalsIgnoreCase("id")) {
         data.getId().formatTo(sb);
         return sb;
      } else if (key.equalsIgnoreCase("id.name")) {
         return this.appendOrNull(data.getId().getName(), sb);
      } else if (key.equalsIgnoreCase("type")) {
         return this.appendOrNull(data.getType(), sb);
      } else if (key.equalsIgnoreCase("message")) {
         data.formatTo(sb);
         return sb;
      } else {
         return this.appendOrNull(data.get(key), sb);
      }
   }

   private StringBuilder getStringBuilder() {
      StringBuilder result = threadLocalStringBuilder.get();
      if (result == null) {
         result = new StringBuilder();
         threadLocalStringBuilder.set(result);
      }

      StringBuilders.trimToMaxSize(result, 2048);
      result.setLength(0);
      return result;
   }

   private StringBuilder appendOrNull(final String value, final StringBuilder sb) {
      if (value == null) {
         return null;
      }

      sb.append(value);
      return sb;
   }

   private boolean listContainsValue(final List<String> candidates, final StringBuilder toMatch) {
      if (toMatch == null) {
         for (int i = 0; i < candidates.size(); i++) {
            String candidate = candidates.get(i);
            if (candidate == null) {
               return true;
            }
         }
      } else {
         for (int i = 0; i < candidates.size(); i++) {
            String candidate = candidates.get(i);
            if (candidate == null) {
               return false;
            }

            if (StringBuilders.equals(candidate, 0, candidate.length(), toMatch, 0, toMatch.length())) {
               return true;
            }
         }
      }

      return false;
   }

   @PluginFactory
   public static StructuredDataFilter createFilter(
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
            LOGGER.error("StructuredDataFilter is not configured with any valid key value pairs");
            return null;
         } else {
            boolean isAnd = operator == null || !operator.equalsIgnoreCase("or");
            return new StructuredDataFilter(map, isAnd, onMatch, onMismatch);
         }
      } else {
         LOGGER.error("keys and values must be specified for the StructuredDataFilter");
         return null;
      }
   }
}
