package org.apache.logging.log4j.core.appender;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.DirectFileRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.DirectWriteRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RollingRandomAccessFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "appender", printObject = true)
@Plugin("RollingRandomAccessFile")
public final class RollingRandomAccessFileAppender extends AbstractOutputStreamAppender<RollingRandomAccessFileManager> {
   private final String fileName;
   private final String filePattern;
   private final Object advertisement;
   private final Advertiser advertiser;

   private RollingRandomAccessFileAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final RollingRandomAccessFileManager manager,
      final String fileName,
      final String filePattern,
      final boolean ignoreExceptions,
      final boolean immediateFlush,
      final int bufferSize,
      final Advertiser advertiser
   ) {
      super(name, layout, filter, ignoreExceptions, immediateFlush, null, manager);
      if (advertiser != null) {
         Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
         configuration.put("contentType", layout.getContentType());
         configuration.put("name", name);
         this.advertisement = advertiser.advertise(configuration);
      } else {
         this.advertisement = null;
      }

      this.fileName = fileName;
      this.filePattern = filePattern;
      this.advertiser = advertiser;
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      this.setStopping();
      super.stop(timeout, timeUnit, false);
      if (this.advertiser != null) {
         this.advertiser.unadvertise(this.advertisement);
      }

      this.setStopped();
      return true;
   }

   @Override
   public void append(final LogEvent event) {
      RollingRandomAccessFileManager manager = this.getManager();
      manager.checkRollover(event);
      super.append(event);
   }

   public String getFileName() {
      return this.fileName;
   }

   public String getFilePattern() {
      return this.filePattern;
   }

   public int getBufferSize() {
      return this.getManager().getBufferSize();
   }

   @PluginFactory
   public static <B extends RollingRandomAccessFileAppender.Builder<B>> B newBuilder() {
      return new RollingRandomAccessFileAppender.Builder<B>().asBuilder();
   }

   public static class Builder<B extends RollingRandomAccessFileAppender.Builder<B>>
      extends AbstractOutputStreamAppender.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<RollingRandomAccessFileAppender> {
      @PluginBuilderAttribute("fileName")
      private String fileName;
      @PluginBuilderAttribute("filePattern")
      private String filePattern;
      @PluginBuilderAttribute("append")
      private boolean append = true;
      @PluginElement("Policy")
      private TriggeringPolicy policy;
      @PluginElement("Strategy")
      private RolloverStrategy strategy;
      @PluginBuilderAttribute("advertise")
      private boolean advertise;
      @PluginBuilderAttribute("advertiseURI")
      private String advertiseURI;
      @PluginBuilderAttribute
      private String filePermissions;
      @PluginBuilderAttribute
      private String fileOwner;
      @PluginBuilderAttribute
      private String fileGroup;

      public Builder() {
         this.setBufferSize(262144);
         this.setIgnoreExceptions(true);
         this.setImmediateFlush(true);
      }

      public RollingRandomAccessFileAppender build() {
         String name = this.getName();
         if (name == null) {
            RollingRandomAccessFileAppender.LOGGER.error("No name provided for FileAppender");
            return null;
         }

         if (this.strategy == null) {
            if (this.fileName != null) {
               this.strategy = DefaultRolloverStrategy.newBuilder().setCompressionLevelStr(String.valueOf(-1)).setConfig(this.getConfiguration()).build();
            } else {
               this.strategy = DirectWriteRolloverStrategy.newBuilder().setCompressionLevelStr(String.valueOf(-1)).setConfig(this.getConfiguration()).build();
            }
         } else if (this.fileName == null && !(this.strategy instanceof DirectFileRolloverStrategy)) {
            RollingRandomAccessFileAppender.LOGGER
               .error("RollingFileAppender '{}': When no file name is provided a DirectFileRolloverStrategy must be configured", name);
            return null;
         }

         if (this.filePattern == null) {
            RollingRandomAccessFileAppender.LOGGER.error("No filename pattern provided for FileAppender with name " + name);
            return null;
         }

         if (this.policy == null) {
            RollingRandomAccessFileAppender.LOGGER.error("A TriggeringPolicy must be provided");
            return null;
         }

         Layout layout = this.getOrCreateLayout();
         boolean immediateFlush = this.isImmediateFlush();
         int bufferSize = this.getBufferSize();
         RollingRandomAccessFileManager manager = RollingRandomAccessFileManager.getRollingRandomAccessFileManager(
            this.fileName,
            this.filePattern,
            this.append,
            immediateFlush,
            bufferSize,
            this.policy,
            this.strategy,
            this.advertiseURI,
            layout,
            this.filePermissions,
            this.fileOwner,
            this.fileGroup,
            this.getConfiguration()
         );
         if (manager == null) {
            return null;
         }

         manager.initialize();
         return new RollingRandomAccessFileAppender(
            name,
            layout,
            this.getFilter(),
            manager,
            this.fileName,
            this.filePattern,
            this.isIgnoreExceptions(),
            immediateFlush,
            bufferSize,
            this.advertise ? this.getConfiguration().getAdvertiser() : null
         );
      }

      public B setFileName(final String fileName) {
         this.fileName = fileName;
         return this.asBuilder();
      }

      public B setFilePattern(final String filePattern) {
         this.filePattern = filePattern;
         return this.asBuilder();
      }

      public B setAppend(final boolean append) {
         this.append = append;
         return this.asBuilder();
      }

      public B setPolicy(final TriggeringPolicy policy) {
         this.policy = policy;
         return this.asBuilder();
      }

      public B setStrategy(final RolloverStrategy strategy) {
         this.strategy = strategy;
         return this.asBuilder();
      }

      public B setAdvertise(final boolean advertise) {
         this.advertise = advertise;
         return this.asBuilder();
      }

      public B setAdvertiseURI(final String advertiseURI) {
         this.advertiseURI = advertiseURI;
         return this.asBuilder();
      }

      public B setFilePermissions(final String filePermissions) {
         this.filePermissions = filePermissions;
         return this.asBuilder();
      }

      public B setFileOwner(final String fileOwner) {
         this.fileOwner = fileOwner;
         return this.asBuilder();
      }

      public B setFileGroup(final String fileGroup) {
         this.fileGroup = fileGroup;
         return this.asBuilder();
      }
   }
}
