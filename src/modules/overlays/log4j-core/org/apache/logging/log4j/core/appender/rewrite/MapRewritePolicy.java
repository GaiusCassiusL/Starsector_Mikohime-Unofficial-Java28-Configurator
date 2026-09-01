package org.apache.logging.log4j.core.appender.rewrite;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(elementType = "rewritePolicy", printObject = true)
@Plugin
public final class MapRewritePolicy implements RewritePolicy {
   protected static final Logger LOGGER = StatusLogger.getLogger();
   private final Map<String, Object> map;
   private final MapRewritePolicy.Mode mode;

   private MapRewritePolicy(final Map<String, Object> map, final MapRewritePolicy.Mode mode) {
      this.map = map;
      this.mode = mode;
   }

   @Override
   public LogEvent rewrite(final LogEvent source) {
      Message msg = source.getMessage();
      if (msg != null && msg instanceof MapMessage) {
         MapMessage<?, Object> mapMsg = (MapMessage<?, Object>)msg;
         Map<String, Object> newMap = new HashMap<>(mapMsg.getData());
         switch (this.mode) {
            case Add:
               newMap.putAll(this.map);
               break;
            default:
               for (Entry<String, Object> entry : this.map.entrySet()) {
                  if (newMap.containsKey(entry.getKey())) {
                     newMap.put(entry.getKey(), entry.getValue());
                  }
               }
         }

         Message message = mapMsg.newInstance(newMap);
         return new Log4jLogEvent.Builder(source).setMessage(message).build();
      } else {
         return source;
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("mode=").append(this.mode);
      sb.append(" {");
      boolean first = true;

      for (Entry<String, Object> entry : this.map.entrySet()) {
         if (!first) {
            sb.append(", ");
         }

         sb.append(entry.getKey()).append('=').append(entry.getValue());
         first = false;
      }

      sb.append('}');
      return sb.toString();
   }

   @PluginFactory
   public static MapRewritePolicy createPolicy(@PluginAttribute final String mode, @PluginElement("KeyValuePair") final KeyValuePair[] pairs) {
      MapRewritePolicy.Mode op;
      op = mode == null ? (op = MapRewritePolicy.Mode.Add) : MapRewritePolicy.Mode.valueOf(mode);
      if (pairs != null && pairs.length != 0) {
         Map<String, Object> map = new HashMap<>();

         for (KeyValuePair pair : pairs) {
            String key = pair.getKey();
            if (key == null) {
               LOGGER.error("A null key is not valid in MapRewritePolicy");
            } else {
               String value = pair.getValue();
               if (value == null) {
                  LOGGER.error("A null value for key " + key + " is not allowed in MapRewritePolicy");
               } else {
                  map.put(pair.getKey(), pair.getValue());
               }
            }
         }

         if (map.isEmpty()) {
            LOGGER.error("MapRewritePolicy is not configured with any valid key value pairs");
            return null;
         } else {
            return new MapRewritePolicy(map, op);
         }
      } else {
         LOGGER.error("keys and values must be specified for the MapRewritePolicy");
         return null;
      }
   }

   public enum Mode {
      Add,
      Update;
   }
}
