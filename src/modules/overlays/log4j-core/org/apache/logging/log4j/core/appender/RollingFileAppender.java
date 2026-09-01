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
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

@Configurable(elementType = "appender", printObject = true)
@Plugin("RollingFile")
public final class RollingFileAppender extends AbstractOutputStreamAppender<RollingFileManager> {
   public static final String PLUGIN_NAME = "RollingFile";
   private final String fileName;
   private final String filePattern;
   private Object advertisement;
   private final Advertiser advertiser;

   private RollingFileAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final RollingFileManager manager,
      final String fileName,
      final String filePattern,
      final boolean ignoreExceptions,
      final boolean immediateFlush,
      final Advertiser advertiser
   ) {
      super(name, layout, filter, ignoreExceptions, immediateFlush, null, manager);
      if (advertiser != null) {
         Map<String, String> configuration = new HashMap<>(layout.getContentFormat());
         configuration.put("contentType", layout.getContentType());
         configuration.put("name", name);
         this.advertisement = advertiser.advertise(configuration);
      }

      this.fileName = fileName;
      this.filePattern = filePattern;
      this.advertiser = advertiser;
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      this.setStopping();
      boolean stopped = super.stop(timeout, timeUnit, false);
      if (this.advertiser != null) {
         this.advertiser.unadvertise(this.advertisement);
      }

      this.setStopped();
      return stopped;
   }

   @Override
   public void append(final LogEvent event) {
      this.getManager().checkRollover(event);
      super.append(event);
   }

   public String getFileName() {
      return this.fileName;
   }

   public String getFilePattern() {
      return this.filePattern;
   }

   public <T extends TriggeringPolicy> T getTriggeringPolicy() {
      return this.getManager().getTriggeringPolicy();
   }

   @PluginFactory
   public static <B extends RollingFileAppender.Builder<B>> B newBuilder() {
      return new RollingFileAppender.Builder<B>().asBuilder();
   }

   public static class Builder<B extends RollingFileAppender.Builder<B>>
      extends AbstractOutputStreamAppender.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<RollingFileAppender> {
      @PluginBuilderAttribute
      private String fileName;
      @PluginBuilderAttribute
      @Required
      private String filePattern;
      @PluginBuilderAttribute
      private boolean append = true;
      @PluginBuilderAttribute
      private boolean locking;
      @PluginElement("Policy")
      @Required
      private TriggeringPolicy policy;
      @PluginElement("Strategy")
      private RolloverStrategy strategy;
      @PluginBuilderAttribute
      private boolean advertise;
      @PluginBuilderAttribute
      private String advertiseUri;
      @PluginBuilderAttribute
      private boolean createOnDemand;
      @PluginBuilderAttribute
      private String filePermissions;
      @PluginBuilderAttribute
      private String fileOwner;
      @PluginBuilderAttribute
      private String fileGroup;

      public RollingFileAppender build() {
         boolean isBufferedIo = this.isBufferedIo();
         int bufferSize = this.getBufferSize();
         if (this.getName() == null) {
            RollingFileAppender.LOGGER.error("RollingFileAppender '{}': No name provided.", this.getName());
            return null;
         }

         if (!isBufferedIo && bufferSize > 0) {
            RollingFileAppender.LOGGER.warn("RollingFileAppender '{}': The bufferSize is set to {} but bufferedIO is not true", this.getName(), bufferSize);
         }

         if (this.filePattern == null) {
            RollingFileAppender.LOGGER.error("RollingFileAppender '{}': No file name pattern provided.", this.getName());
            return null;
         }

         if (this.policy == null) {
            RollingFileAppender.LOGGER.error("RollingFileAppender '{}': No TriggeringPolicy provided.", this.getName());
            return null;
         }

         if (this.strategy == null) {
            if (this.fileName != null) {
               this.strategy = DefaultRolloverStrategy.newBuilder().setCompressionLevelStr(String.valueOf(-1)).setConfig(this.getConfiguration()).build();
            } else {
               this.strategy = DirectWriteRolloverStrategy.newBuilder().setCompressionLevelStr(String.valueOf(-1)).setConfig(this.getConfiguration()).build();
            }
         } else if (this.fileName == null && !(this.strategy instanceof DirectFileRolloverStrategy)) {
            RollingFileAppender.LOGGER
               .error(
                  "RollingFileAppender '{}': When no file name is provided a {} must be configured",
                  this.getName(),
                  DirectFileRolloverStrategy.class.getSimpleName()
               );
            return null;
         }

         Layout layout = this.getOrCreateLayout();
         RollingFileManager manager = RollingFileManager.getFileManager(
            this.fileName,
            this.filePattern,
            this.append,
            isBufferedIo,
            this.policy,
            this.strategy,
            this.advertiseUri,
            layout,
            bufferSize,
            this.isImmediateFlush(),
            this.createOnDemand,
            this.filePermissions,
            this.fileOwner,
            this.fileGroup,
            this.getConfiguration()
         );
         if (manager == null) {
            return null;
         }

         manager.initialize();
         return new RollingFileAppender(
            this.getName(),
            layout,
            this.getFilter(),
            manager,
            this.fileName,
            this.filePattern,
            this.isIgnoreExceptions(),
            !isBufferedIo || this.isImmediateFlush(),
            this.advertise ? this.getConfiguration().getAdvertiser() : null
         );
      }

      public String getAdvertiseUri() {
         return this.advertiseUri;
      }

      public String getFileName() {
         return this.fileName;
      }

      public boolean isAdvertise() {
         return this.advertise;
      }

      public boolean isAppend() {
         return this.append;
      }

      public boolean isCreateOnDemand() {
         return this.createOnDemand;
      }

      public boolean isLocking() {
         return this.locking;
      }

      public String getFilePermissions() {
         return this.filePermissions;
      }

      public String getFileOwner() {
         return this.fileOwner;
      }

      public String getFileGroup() {
         return this.fileGroup;
      }

      public B setAdvertise(final boolean advertise) {
         this.advertise = advertise;
         return this.asBuilder();
      }

      public B setAdvertiseUri(final String advertiseUri) {
         this.advertiseUri = advertiseUri;
         return this.asBuilder();
      }

      public B setAppend(final boolean append) {
         this.append = append;
         return this.asBuilder();
      }

      public B setFileName(final String fileName) {
         this.fileName = fileName;
         return this.asBuilder();
      }

      public B setCreateOnDemand(final boolean createOnDemand) {
         this.createOnDemand = createOnDemand;
         return this.asBuilder();
      }

      public B setLocking(final boolean locking) {
         this.locking = locking;
         return this.asBuilder();
      }

      public String getFilePattern() {
         return this.filePattern;
      }

      public TriggeringPolicy getPolicy() {
         return this.policy;
      }

      public RolloverStrategy getStrategy() {
         return this.strategy;
      }

      public B setFilePattern(final String filePattern) {
         this.filePattern = filePattern;
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
