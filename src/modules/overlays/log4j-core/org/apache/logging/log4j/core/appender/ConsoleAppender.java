package org.apache.logging.log4j.core.appender;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.impl.Log4jPropertyKey;
import org.apache.logging.log4j.core.util.CloseShieldOutputStream;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Throwables;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.PropertyEnvironment;
import org.apache.logging.log4j.util.SystemPropertiesPropertySource;

@Configurable(elementType = "appender", printObject = true)
@Plugin("Console")
public final class ConsoleAppender extends AbstractOutputStreamAppender<OutputStreamManager> {
   public static final String PLUGIN_NAME = "Console";
   private static final String JANSI_CLASS = "org.fusesource.jansi.WindowsAnsiOutputStream";
   private static final ConsoleAppender.ConsoleManagerFactory factory = new ConsoleAppender.ConsoleManagerFactory();
   private static final ConsoleAppender.Target DEFAULT_TARGET = ConsoleAppender.Target.SYSTEM_OUT;
   private static final AtomicInteger COUNT = new AtomicInteger();
   private final ConsoleAppender.Target target;
   private static final PropertiesUtil sysProps = new PropertiesUtil(new SystemPropertiesPropertySource());

   private ConsoleAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final OutputStreamManager manager,
      final boolean ignoreExceptions,
      final ConsoleAppender.Target target,
      final Property[] properties
   ) {
      super(name, layout, filter, ignoreExceptions, true, properties, manager);
      this.target = target;
   }

   public static ConsoleAppender createDefaultAppenderForLayout(final Layout layout) {
      return new ConsoleAppender(
         "DefaultConsole-" + COUNT.incrementAndGet(), layout, null, getDefaultManager(DEFAULT_TARGET, false, false, layout), true, DEFAULT_TARGET, null
      );
   }

   @PluginFactory
   public static <B extends ConsoleAppender.Builder<B>> B newBuilder() {
      return new ConsoleAppender.Builder<B>().asBuilder();
   }

   private static OutputStreamManager getDefaultManager(final ConsoleAppender.Target target, final boolean follow, final boolean direct, final Layout layout) {
      OutputStream os = getOutputStream(follow, direct, target, PropertiesUtil.getProperties());
      String managerName = target.name() + "." + follow + "." + direct + "-" + COUNT.get();
      return OutputStreamManager.getManager(managerName, new ConsoleAppender.FactoryData(os, managerName, layout), factory);
   }

   private static OutputStreamManager getManager(
      final ConsoleAppender.Target target, final boolean follow, final boolean direct, final Layout layout, final PropertyEnvironment properties
   ) {
      OutputStream os = getOutputStream(follow, direct, target, properties);
      String managerName = target.name() + "." + follow + "." + direct;
      return OutputStreamManager.getManager(managerName, new ConsoleAppender.FactoryData(os, managerName, layout), factory);
   }

   private static OutputStream getOutputStream(
      final boolean follow, final boolean direct, final ConsoleAppender.Target target, final PropertyEnvironment properties
   ) {
      String enc = Charset.defaultCharset().name();

      OutputStream outputStream;
      try {
         outputStream = target == ConsoleAppender.Target.SYSTEM_OUT
            ? (direct ? new FileOutputStream(FileDescriptor.out) : (follow ? new PrintStream(new ConsoleAppender.SystemOutStream(), true, enc) : System.out))
            : (direct ? new FileOutputStream(FileDescriptor.err) : (follow ? new PrintStream(new ConsoleAppender.SystemErrStream(), true, enc) : System.err));
         outputStream = new CloseShieldOutputStream(outputStream);
      } catch (UnsupportedEncodingException ex) {
         throw new IllegalStateException("Unsupported default encoding " + enc, ex);
      }

      if (properties.isOsWindows() && !properties.getBooleanProperty(Log4jPropertyKey.CONSOLE_JANSI_ENABLED, true) && !direct) {
         try {
            Class<?> clazz = Loader.loadClass("org.fusesource.jansi.WindowsAnsiOutputStream");
            Constructor<?> constructor = clazz.getConstructor(OutputStream.class);
            return new CloseShieldOutputStream((OutputStream)constructor.newInstance(outputStream));
         } catch (ClassNotFoundException cnfe) {
            LOGGER.debug("Jansi is not installed, cannot find {}", "org.fusesource.jansi.WindowsAnsiOutputStream");
         } catch (NoSuchMethodException nsme) {
            LOGGER.warn("{} is missing the proper constructor", "org.fusesource.jansi.WindowsAnsiOutputStream");
         } catch (Exception ex) {
            LOGGER.warn(
               "Unable to instantiate {} due to {}", "org.fusesource.jansi.WindowsAnsiOutputStream", clean(Throwables.getRootCause(ex).toString()).trim()
            );
         }

         return outputStream;
      } else {
         return outputStream;
      }
   }

   private static String clean(final String string) {
      return string.replace('\u0000', ' ');
   }

   public ConsoleAppender.Target getTarget() {
      return this.target;
   }

   public static class Builder<B extends ConsoleAppender.Builder<B>>
      extends AbstractOutputStreamAppender.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<ConsoleAppender> {
      @PluginBuilderAttribute
      @Required
      private ConsoleAppender.Target target = ConsoleAppender.DEFAULT_TARGET;
      @PluginBuilderAttribute
      private boolean follow;
      @PluginBuilderAttribute
      private boolean direct;
      @PluginConfiguration
      private Configuration configuration;

      public B setTarget(final ConsoleAppender.Target aTarget) {
         this.target = aTarget;
         return this.asBuilder();
      }

      public B setFollow(final boolean shouldFollow) {
         this.follow = shouldFollow;
         return this.asBuilder();
      }

      public B setDirect(final boolean shouldDirect) {
         this.direct = shouldDirect;
         return this.asBuilder();
      }

      public B setConfiguration(final Configuration configuration) {
         this.configuration = configuration;
         return this.asBuilder();
      }

      public ConsoleAppender build() {
         if (this.follow && this.direct) {
            throw new IllegalArgumentException("Cannot use both follow and direct on ConsoleAppender '" + this.getName() + "'");
         }

         Layout layout = this.getOrCreateLayout(this.target.getDefaultCharset());
         PropertyEnvironment propertyEnvironment = this.configuration != null && this.configuration.getLoggerContext() != null
            ? this.configuration.getLoggerContext().getProperties()
            : PropertiesUtil.getProperties();
         return new ConsoleAppender(
            this.getName(),
            layout,
            this.getFilter(),
            ConsoleAppender.getManager(this.target, this.follow, this.direct, layout, propertyEnvironment),
            this.isIgnoreExceptions(),
            this.target,
            this.getPropertyArray()
         );
      }
   }

   private static class ConsoleManagerFactory implements ManagerFactory<OutputStreamManager, ConsoleAppender.FactoryData> {
      public OutputStreamManager createManager(final String name, final ConsoleAppender.FactoryData data) {
         return new OutputStreamManager(data.os, data.name, data.layout, true);
      }
   }

   private static class FactoryData {
      private final OutputStream os;
      private final String name;
      private final Layout layout;

      public FactoryData(final OutputStream os, final String type, final Layout layout) {
         this.os = os;
         this.name = type;
         this.layout = layout;
      }
   }

   private static class SystemErrStream extends OutputStream {
      public SystemErrStream() {
      }

      @Override
      public void close() {
      }

      @Override
      public void flush() {
         System.err.flush();
      }

      @Override
      public void write(final byte[] b) throws IOException {
         System.err.write(b);
      }

      @Override
      public void write(final byte[] b, final int off, final int len) throws IOException {
         System.err.write(b, off, len);
      }

      @Override
      public void write(final int b) {
         System.err.write(b);
      }
   }

   private static class SystemOutStream extends OutputStream {
      public SystemOutStream() {
      }

      @Override
      public void close() {
      }

      @Override
      public void flush() {
         System.out.flush();
      }

      @Override
      public void write(final byte[] b) throws IOException {
         System.out.write(b);
      }

      @Override
      public void write(final byte[] b, final int off, final int len) throws IOException {
         System.out.write(b, off, len);
      }

      @Override
      public void write(final int b) throws IOException {
         System.out.write(b);
      }
   }

   public enum Target {
      SYSTEM_OUT {
         @Override
         public Charset getDefaultCharset() {
            return this.getCharset("sun.stdout.encoding", Charset.defaultCharset());
         }
      },
      SYSTEM_ERR {
         @Override
         public Charset getDefaultCharset() {
            return this.getCharset("sun.stderr.encoding", Charset.defaultCharset());
         }
      };

      public abstract Charset getDefaultCharset();

      protected Charset getCharset(final String property, final Charset defaultCharset) {
         return ConsoleAppender.sysProps.getCharsetProperty(property, defaultCharset);
      }
   }
}
