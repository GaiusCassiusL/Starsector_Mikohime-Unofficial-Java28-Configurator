package org.apache.logging.log4j.core.lookup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.Strings;

@Lookup
@Plugin("map")
public class MapLookup implements StrLookup {
   private final Map<String, String> map;

   public MapLookup() {
      this.map = null;
   }

   public MapLookup(final Map<String, String> map) {
      this.map = map;
   }

   static Map<String, String> initMap(final String[] srcArgs, final Map<String, String> destMap) {
      for (int i = 0; i < srcArgs.length; i++) {
         int next = i + 1;
         String value = srcArgs[i];
         destMap.put(Integer.toString(i), value);
         destMap.put(value, next < srcArgs.length ? srcArgs[next] : null);
      }

      return destMap;
   }

   static HashMap<String, String> newMap(final int initialCapacity) {
      return new HashMap<>(initialCapacity);
   }

   static Map<String, String> toMap(final List<String> args) {
      if (args == null) {
         return null;
      }

      int size = args.size();
      return initMap(args.toArray(Strings.EMPTY_ARRAY), newMap(size));
   }

   static Map<String, String> toMap(final String[] args) {
      return args == null ? null : initMap(args, newMap(args.length));
   }

   protected Map<String, String> getMap() {
      return this.map;
   }

   @Override
   public String lookup(final LogEvent event, final String key) {
      boolean isMapMessage = event != null && event.getMessage() instanceof MapMessage;
      if (isMapMessage) {
         String obj = ((MapMessage)event.getMessage()).get(key);
         if (obj != null) {
            return obj;
         }
      }

      return this.map != null ? this.map.get(key) : null;
   }

   @Override
   public String lookup(final String key) {
      return key != null && this.map != null ? this.map.get(key) : null;
   }
}
