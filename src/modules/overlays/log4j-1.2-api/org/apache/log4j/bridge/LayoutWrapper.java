package org.apache.log4j.bridge;

import org.apache.log4j.Layout;
import org.apache.log4j.spi.LoggingEvent;

public class LayoutWrapper extends Layout {
   private final org.apache.logging.log4j.core.Layout layout;

   public static Layout adapt(final org.apache.logging.log4j.core.Layout layout) {
      if (layout instanceof LayoutAdapter) {
         return ((LayoutAdapter)layout).getLayout();
      } else {
         return layout != null ? new LayoutWrapper(layout) : null;
      }
   }

   public LayoutWrapper(final org.apache.logging.log4j.core.Layout layout) {
      this.layout = layout;
   }

   @Override
   public String format(final LoggingEvent event) {
      return this.layout.toSerializable(((LogEventAdapter)event).getEvent()).toString();
   }

   public org.apache.logging.log4j.core.Layout getLayout() {
      return this.layout;
   }

   @Override
   public boolean ignoresThrowable() {
      return false;
   }

   @Override
   public String toString() {
      return String.format("LayoutWrapper [layout=%s]", this.layout);
   }
}
