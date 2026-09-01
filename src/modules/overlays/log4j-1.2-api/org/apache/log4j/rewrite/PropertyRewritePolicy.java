package org.apache.log4j.rewrite;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Map.Entry;
import org.apache.log4j.bridge.LogEventAdapter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LocationInfo;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent.Builder;
import org.apache.logging.log4j.message.SimpleMessage;
import org.apache.logging.log4j.util.SortedArrayStringMap;

public class PropertyRewritePolicy implements RewritePolicy {
   private Map<String, String> properties = Collections.EMPTY_MAP;

   public void setProperties(final String properties) {
      Map<String, String> newMap = new HashMap<>();
      StringTokenizer pairs = new StringTokenizer(properties, ",");

      while (pairs.hasMoreTokens()) {
         StringTokenizer entry = new StringTokenizer(pairs.nextToken(), "=");
         newMap.put(entry.nextElement().toString().trim(), entry.nextElement().toString().trim());
      }

      synchronized (this) {
         this.properties = newMap;
      }
   }

   @Override
   public LoggingEvent rewrite(final LoggingEvent source) {
      if (!this.properties.isEmpty()) {
         Map<String, String> rewriteProps = source.getProperties() != null ? new HashMap<>(source.getProperties()) : new HashMap<>();

         for (Entry<String, String> entry : this.properties.entrySet()) {
            if (!rewriteProps.containsKey(entry.getKey())) {
               rewriteProps.put(entry.getKey(), entry.getValue());
            }
         }

         LogEvent event;
         if (source instanceof LogEventAdapter) {
            event = new Builder(((LogEventAdapter)source).getEvent()).setContextData(new SortedArrayStringMap(rewriteProps)).build();
         } else {
            LocationInfo info = source.getLocationInformation();
            StackTraceElement element = new StackTraceElement(
               info.getClassName(), info.getMethodName(), info.getFileName(), Integer.parseInt(info.getLineNumber())
            );
            Thread thread = this.getThread(source.getThreadName());
            long threadId = thread != null ? thread.getId() : 0L;
            int threadPriority = thread != null ? thread.getPriority() : 0;
            event = Log4jLogEvent.newBuilder()
               .setContextData(new SortedArrayStringMap(rewriteProps))
               .setLevel(OptionConverter.convertLevel(source.getLevel()))
               .setLoggerFqcn(source.getFQNOfLoggerClass())
               .setMarker(null)
               .setMessage(new SimpleMessage(source.getRenderedMessage()))
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
