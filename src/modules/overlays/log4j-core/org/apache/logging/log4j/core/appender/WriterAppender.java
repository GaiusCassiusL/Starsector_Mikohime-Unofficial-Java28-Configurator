package org.apache.logging.log4j.core.appender;

import java.io.Writer;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.CloseShieldWriter;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "appender", printObject = true)
@Plugin("Writer")
public final class WriterAppender extends AbstractWriterAppender<WriterManager> {
   private static final WriterAppender.WriterManagerFactory factory = new WriterAppender.WriterManagerFactory();

   @PluginFactory
   public static WriterAppender createAppender(
      StringLayout layout, final Filter filter, final Writer target, final String name, final boolean follow, final boolean ignore
   ) {
      if (name == null) {
         LOGGER.error("No name provided for WriterAppender");
         return null;
      }

      if (layout == null) {
         layout = PatternLayout.createDefaultLayout();
      }

      return new WriterAppender(name, layout, filter, getManager(target, follow, layout), ignore, Property.EMPTY_ARRAY);
   }

   private static WriterManager getManager(final Writer target, final boolean follow, final StringLayout layout) {
      Writer writer = new CloseShieldWriter(target);
      String managerName = target.getClass().getName() + "@" + Integer.toHexString(target.hashCode()) + "." + follow;
      return WriterManager.getManager(managerName, new WriterAppender.FactoryData(writer, managerName, layout), factory);
   }

   @PluginFactory
   public static <B extends WriterAppender.Builder<B>> B newBuilder() {
      return new WriterAppender.Builder<B>().asBuilder();
   }

   private WriterAppender(
      final String name,
      final StringLayout layout,
      final Filter filter,
      final WriterManager manager,
      final boolean ignoreExceptions,
      final Property[] properties
   ) {
      super(name, layout, filter, ignoreExceptions, true, properties, manager);
   }

   public static class Builder<B extends WriterAppender.Builder<B>>
      extends AbstractAppender.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<WriterAppender> {
      private boolean follow = false;
      private Writer target;

      public WriterAppender build() {
         StringLayout layout = (StringLayout)this.getLayout();
         StringLayout actualLayout = layout != null ? layout : PatternLayout.createDefaultLayout();
         return new WriterAppender(
            this.getName(),
            actualLayout,
            this.getFilter(),
            WriterAppender.getManager(this.target, this.follow, actualLayout),
            this.isIgnoreExceptions(),
            this.getPropertyArray()
         );
      }

      public B setFollow(final boolean shouldFollow) {
         this.follow = shouldFollow;
         return this.asBuilder();
      }

      public B setTarget(final Writer aTarget) {
         this.target = aTarget;
         return this.asBuilder();
      }
   }

   private static class FactoryData {
      private final StringLayout layout;
      private final String name;
      private final Writer writer;

      public FactoryData(final Writer writer, final String type, final StringLayout layout) {
         this.writer = writer;
         this.name = type;
         this.layout = layout;
      }
   }

   private static class WriterManagerFactory implements ManagerFactory<WriterManager, WriterAppender.FactoryData> {
      public WriterManager createManager(final String name, final WriterAppender.FactoryData data) {
         return new WriterManager(data.writer, data.name, data.layout, true);
      }
   }
}
