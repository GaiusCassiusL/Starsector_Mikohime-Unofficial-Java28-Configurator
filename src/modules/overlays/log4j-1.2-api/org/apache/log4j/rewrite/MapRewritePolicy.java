package org.apache.log4j.rewrite;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.log4j.bridge.LogEventAdapter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.MapMessage;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;

public class MapRewritePolicy implements RewritePolicy {
   @Override
   public LoggingEvent rewrite(final LoggingEvent source) {
      Object msg = source.getMessage();
      if (!(msg instanceof MapMessage) && !(msg instanceof Map)) {
         return source;
      }

      Map<String, String> props = source.getProperties() != null ? new HashMap<>(source.getProperties()) : new HashMap<>();
      Map<String, Object> eventProps = msg instanceof Map ? (Map)msg : ((MapMessage)msg).getData();
      Message newMessage = null;
      Object newMsg = eventProps.get("message");
      if (newMsg != null) {
         Message var14 = new SimpleMessage(newMsg.toString());

         for (Entry<String, Object> entry : eventProps.entrySet()) {
            if (!"message".equals(entry.getKey())) {
               props.put(entry.getKey(), entry.getValue().toString());
            }
         }

         LogEvent event;
         if (source instanceof LogEventAdapter) {
            event = new Builder(((LogEventAdapter)source).getEvent()).setMessage(var14).setContextData(new SortedArrayStringMap(props)).build();
         } else {
            LocationInfo info = source.getLocationInformation();
            StackTraceElement element = new StackTraceElement(
               info.getClassName(), info.getMethodName(), info.getFileName(), Integer.parseInt(info.getLineNumber())
            );
            Thread thread = this.getThread(source.getThreadName());
            long threadId = thread != null ? thread.getId() : 0L;
            int threadPriority = thread != null ? thread.getPriority() : 0;
            event = Log4jLogEvent.newBuilder()
               .setContextData(new SortedArrayStringMap(props))
               .setLevel(OptionConverter.convertLevel(source.getLevel()))
               .setLoggerFqcn(source.getFQNOfLoggerClass())
               .setMarker(null)
               .setMessage(var14)
               .setSource(element)
               .setLoggerName(source.getLoggerName())
               .setThreadName(source.getThreadName())
               .setThreadId(threadId)
               .setThreadPriority(threadPriority)
               .setThrown(source.getThrowableInformation().getThrowable())
               .setTimeMillis(source.getTimeStamp())
               .setNanoTime(0L)
               .setThrownProxy(null)
               .build();
         }

         return new LogEventAdapter(event);
      } else {
         return source;
      }
   }

   private Thread getThread(final String name) {
      for (Thread thread : Thread.getAllStackTraces().keySet()) {
         if (thread.getName().equals(name)) {
            return thread;
         }
      }

      return null;
   }
}
