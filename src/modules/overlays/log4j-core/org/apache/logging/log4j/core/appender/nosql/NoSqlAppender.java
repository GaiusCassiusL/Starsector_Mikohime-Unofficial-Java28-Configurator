package org.apache.logging.log4j.core.appender.nosql;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.appender.db.AbstractDatabaseAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "appender", printObject = true)
@Plugin("NoSql")
public final class NoSqlAppender extends AbstractDatabaseAppender<NoSqlDatabaseManager<?>> {
   private final String description = this.getName() + "{ manager=" + this.getManager() + " }";

   @PluginFactory
   public static <B extends NoSqlAppender.Builder<B>> B newBuilder() {
      return new NoSqlAppender.Builder<B>().asBuilder();
   }

   private NoSqlAppender(
      final String name,
      final Filter filter,
      final Layout layout,
      final boolean ignoreExceptions,
      final Property[] properties,
      final NoSqlDatabaseManager<?> manager
   ) {
      super(name, filter, layout, ignoreExceptions, properties, manager);
   }

   @Override
   public String toString() {
      return this.description;
   }

   public static class Builder<B extends NoSqlAppender.Builder<B>>
      extends AbstractAppender.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<NoSqlAppender> {
      @PluginBuilderAttribute("bufferSize")
      private int bufferSize;
      @PluginElement("NoSqlProvider")
      private NoSqlProvider<?> provider;

      public NoSqlAppender build() {
         String name = this.getName();
         if (this.provider == null) {
            NoSqlAppender.LOGGER.error("NoSQL provider not specified for appender [{}].", name);
            return null;
         } else {
            String managerName = "noSqlManager{ description=" + name + ", bufferSize=" + this.bufferSize + ", provider=" + this.provider + " }";
            NoSqlDatabaseManager<?> manager = NoSqlDatabaseManager.getNoSqlDatabaseManager(managerName, this.bufferSize, this.provider);
            return manager == null
               ? null
               : new NoSqlAppender(name, this.getFilter(), this.getLayout(), this.isIgnoreExceptions(), this.getPropertyArray(), manager);
         }
      }

      public B setBufferSize(final int bufferSize) {
         this.bufferSize = bufferSize;
         return this.asBuilder();
      }

      public B setProvider(final NoSqlProvider<?> provider) {
         this.provider = provider;
         return this.asBuilder();
      }
   }
}
