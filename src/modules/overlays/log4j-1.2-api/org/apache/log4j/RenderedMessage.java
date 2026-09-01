package org.apache.log4j;

import org.apache.log4j.or.ObjectRenderer;
import org.apache.logging.log4j.message.Message;

public class RenderedMessage implements Message {
   private final ObjectRenderer renderer;
   private final Object object;
   private String rendered = null;

   public RenderedMessage(final ObjectRenderer renderer, final Object object) {
      this.renderer = renderer;
      this.object = object;
   }

   public String getFormattedMessage() {
      if (this.rendered == null) {
         this.rendered = this.renderer.doRender(this.object);
      }

      return this.rendered;
   }

   public String getFormat() {
      return this.getFormattedMessage();
   }

   public Object[] getParameters() {
      return null;
   }

   public Throwable getThrowable() {
      return null;
   }
}
