package org.apache.logging.log4j.core.appender;

import java.io.OutputStream;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.core.util.NullOutputStream;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "appender", printObject = true)
@Plugin("OutputStream")
public final class OutputStreamAppender extends AbstractOutputStreamAppender<OutputStreamManager> {
   private static final OutputStreamAppender.OutputStreamManagerFactory factory = new OutputStreamAppender.OutputStreamManagerFactory();

   @PluginFactory
   public static OutputStreamAppender createAppender(
      Layout layout, final Filter filter, final OutputStream target, final String name, final boolean follow, final boolean ignore
   ) {
      if (name == null) {
         LOGGER.error("No name provided for OutputStreamAppender");
         return null;
      }

      if (layout == null) {
         layout = PatternLayout.createDefaultLayout();
      }

      return new OutputStreamAppender(name, layout, filter, getManager(target, follow, layout), ignore, null);
   }

   private static OutputStreamManager getManager(final OutputStream target, final boolean follow, final Layout layout) {
      OutputStream os = target == null ? NullOutputStream.getInstance() : new CloseShieldOutputStream(target);
      OutputStream targetRef = target == null ? os : target;
      String managerName = targetRef.getClass().getName() + "@" + Integer.toHexString(targetRef.hashCode()) + "." + follow;
      return OutputStreamManager.getManager(managerName, new OutputStreamAppender.FactoryData(os, managerName, layout), factory);
   }

   @PluginFactory
   public static <B extends OutputStreamAppender.Builder<B>> B newBuilder() {
      return new OutputStreamAppender.Builder<B>().asBuilder();
   }

   private OutputStreamAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final OutputStreamManager manager,
      final boolean ignoreExceptions,
      final Property[] properties
   ) {
      super(name, layout, filter, ignoreExceptions, true, null, manager);
   }

   public static class Builder<B extends OutputStreamAppender.Builder<B>>
      extends AbstractOutputStreamAppender.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<OutputStreamAppender> {
      private boolean follow = false;
      private final Layout layout = PatternLayout.createDefaultLayout();
      private OutputStream target;

      public OutputStreamAppender build() {
         return new OutputStreamAppender(
            this.getName(),
            this.layout,
            this.getFilter(),
            OutputStreamAppender.getManager(this.target, this.follow, this.layout),
            this.isIgnoreExceptions(),
            this.getPropertyArray()
         );
      }

      public B setFollow(final boolean shouldFollow) {
         this.follow = shouldFollow;
         return this.asBuilder();
      }

      public B setTarget(final OutputStream aTarget) {
         this.target = aTarget;
         return this.asBuilder();
      }
   }

   private static class FactoryData {
      private final Layout layout;
      private final String name;
      private final OutputStream os;

      public FactoryData(final OutputStream os, final String type, final Layout layout) {
         this.os = os;
         this.name = type;
         this.layout = layout;
      }
   }

   private static class OutputStreamManagerFactory implements ManagerFactory<OutputStreamManager, OutputStreamAppender.FactoryData> {
      public OutputStreamManager createManager(final String name, final OutputStreamAppender.FactoryData data) {
         return new OutputStreamManager(data.os, data.name, data.layout, true);
      }
   }
}
