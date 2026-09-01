package org.apache.logging.log4j.core.appender;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "appender", printObject = true)
@Plugin("CountingNoOp")
public class CountingNoOpAppender extends AbstractAppender {
   private final AtomicLong total = new AtomicLong();

   public CountingNoOpAppender(final String name, final Layout layout) {
      super(name, null, layout, true, Property.EMPTY_ARRAY);
   }

   public long getCount() {
      return this.total.get();
   }

   @Override
   public void append(final LogEvent event) {
      this.total.incrementAndGet();
   }

   @PluginFactory
   public static CountingNoOpAppender createAppender(@PluginAttribute final String name) {
      return new CountingNoOpAppender(Objects.requireNonNull(name), null);
   }
}
