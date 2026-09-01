package org.apache.log4j.bridge;

import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.layout.ByteBufferDestination;

public final class LayoutAdapter implements Layout {
   private final org.apache.log4j.Layout layout;

   public static Layout adapt(final org.apache.log4j.Layout layout) {
      if (layout instanceof LayoutWrapper) {
         return ((LayoutWrapper)layout).getLayout();
      } else {
         return layout != null ? new LayoutAdapter(layout) : null;
      }
   }

   private LayoutAdapter(final org.apache.log4j.Layout layout) {
      this.layout = layout;
   }

   public org.apache.log4j.Layout getLayout() {
      return this.layout;
   }

   public byte[] getFooter() {
      return this.layout.getFooter() == null ? null : this.layout.getFooter().getBytes();
   }

   public byte[] getHeader() {
      return this.layout.getHeader() == null ? null : this.layout.getHeader().getBytes();
   }

   public byte[] toByteArray(final LogEvent event) {
      String result = this.layout.format(new LogEventAdapter(event));
      return result == null ? null : result.getBytes();
   }

   public String toSerializable(final LogEvent event) {
      return this.layout.format(new LogEventAdapter(event));
   }

   public String getContentType() {
      return this.layout.getContentType();
   }

   public Map<String, String> getContentFormat() {
      return new HashMap<>();
   }

   public void encode(final LogEvent event, final ByteBufferDestination destination) {
      byte[] data = this.toByteArray(event);
      destination.writeBytes(data, 0, data.length);
   }
}
