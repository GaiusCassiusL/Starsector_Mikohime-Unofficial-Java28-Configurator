package org.apache.log4j.builders.appender;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderWrapper;
import org.apache.log4j.bridge.LayoutAdapter;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingFileAppender;
import org.apache.logging.log4j.core.appender.RollingFileAppender.Builder;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.DefaultRolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.RolloverStrategy;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.RollingFileAppender")
public class RollingFileAppenderBuilder extends AbstractBuilder implements AppenderBuilder {
   private static final String DEFAULT_MAX_SIZE = "10 MB";
   private static final String DEFAULT_MAX_BACKUPS = "1";
   private static final Logger LOGGER = StatusLogger.getLogger();

   public RollingFileAppenderBuilder() {
   }

   public RollingFileAppenderBuilder(final String prefix, final Properties properties) {
      super(prefix, properties);
   }

   @Override
   public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
      String name = this.getNameAttribute(appenderElement);
      AtomicReference<Layout> layout = new AtomicReference<>();
      AtomicReference<Filter> filter = new AtomicReference<>();
      AtomicReference<String> fileName = new AtomicReference<>();
      AtomicBoolean immediateFlush = new AtomicBoolean(true);
      AtomicBoolean append = new AtomicBoolean(true);
      AtomicBoolean bufferedIo = new AtomicBoolean();
      AtomicInteger bufferSize = new AtomicInteger(8192);
      AtomicReference<String> maxSize = new AtomicReference<>("10 MB");
      AtomicReference<String> maxBackups = new AtomicReference<>("1");
      AtomicReference<String> level = new AtomicReference<>();
      XmlConfiguration.forEachElement(appenderElement.getChildNodes(), currentElement -> {
         switch (currentElement.getTagName()) {
            case "layout":
               layout.set(config.parseLayout(currentElement));
               break;
            case "filter":
               config.addFilter(filter, currentElement);
               break;
            case "param":
               switch (this.getNameAttributeKey(currentElement)) {
                  case "File":
                     this.set("File", currentElement, fileName);
                     break;
                  case "Append":
                     this.set("Append", currentElement, append);
                     break;
                  case "BufferedIO":
                     this.set("BufferedIO", currentElement, bufferedIo);
                     break;
                  case "BufferSize":
                     this.set("BufferSize", currentElement, bufferSize);
                     break;
                  case "MaxBackupIndex":
                     this.set("MaxBackupIndex", currentElement, maxBackups);
                     break;
                  case "MaxFileSize":
                     this.set("MaxFileSize", currentElement, maxSize);
                     break;
                  case "Threshold":
                     this.set("Threshold", currentElement, level);
                     break;
                  case "ImmediateFlush":
                     this.set("ImmediateFlush", currentElement, immediateFlush);
               }
         }
      });
      return this.createAppender(
         name,
         config,
         layout.get(),
         filter.get(),
         append.get(),
         bufferedIo.get(),
         bufferSize.get(),
         immediateFlush.get(),
         fileName.get(),
         level.get(),
         maxSize.get(),
         maxBackups.get(),
         (Clock)config.getComponent(Clock.KEY)
      );
   }

   @Override
   public Appender parseAppender(
      final String name,
      final String appenderPrefix,
      final String layoutPrefix,
      final String filterPrefix,
      final Properties props,
      final PropertiesConfiguration configuration
   ) {
      Layout layout = configuration.parseLayout(layoutPrefix, name, props);
      Filter filter = configuration.parseAppenderFilters(props, filterPrefix, name);
      String fileName = this.getProperty("File");
      String level = this.getProperty("Threshold");
      boolean append = this.getBooleanProperty("Append", true);
      boolean immediateFlush = this.getBooleanProperty("ImmediateFlush", true);
      boolean bufferedIo = this.getBooleanProperty("BufferedIO", false);
      int bufferSize = this.getIntegerProperty("BufferSize", 8192);
      String maxSize = this.getProperty("MaxFileSize", "10 MB");
      String maxBackups = this.getProperty("MaxBackupIndex", "1");
      return this.createAppender(
         name,
         configuration,
         layout,
         filter,
         append,
         bufferedIo,
         bufferSize,
         immediateFlush,
         fileName,
         level,
         maxSize,
         maxBackups,
         (Clock)configuration.getComponent(Clock.KEY)
      );
   }

   private Appender createAppender(
      final String name,
      final Log4j1Configuration config,
      final Layout layout,
      final Filter filter,
      final boolean append,
      final boolean bufferedIo,
      final int bufferSize,
      boolean immediateFlush,
      final String fileName,
      final String level,
      final String maxSize,
      final String maxBackups,
      final Clock clock
   ) {
      org.apache.logging.log4j.core.Layout fileLayout = LayoutAdapter.adapt(layout);
      if (!bufferedIo) {
         immediateFlush = false;
      }

      org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
      if (fileName == null) {
         LOGGER.error("Unable to create RollingFileAppender, no file name provided");
         return null;
      } else {
         String filePattern = fileName + ".%i";
         TriggeringPolicy timePolicy = TimeBasedTriggeringPolicy.newBuilder().setClock(clock).setModulate(true).build();
         SizeBasedTriggeringPolicy sizePolicy = SizeBasedTriggeringPolicy.createPolicy(maxSize);
         CompositeTriggeringPolicy policy = CompositeTriggeringPolicy.createPolicy(new TriggeringPolicy[]{sizePolicy});
         RolloverStrategy strategy = DefaultRolloverStrategy.newBuilder().setConfig(config).setMax(maxBackups).build();
         return AppenderWrapper.adapt(
            ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)RollingFileAppender.newBuilder().setName(name)).setConfiguration(config))
                              .setLayout(fileLayout))
                           .setFilter(fileFilter))
                        .setAppend(append)
                        .setBufferedIo(bufferedIo))
                     .setBufferSize(bufferSize))
                  .setImmediateFlush(immediateFlush))
               .setFileName(fileName)
               .setFilePattern(filePattern)
               .setPolicy(policy)
               .setStrategy(strategy)
               .build()
         );
      }
   }
}
