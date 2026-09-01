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
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.DailyRollingFileAppender")
public class DailyRollingFileAppenderBuilder extends AbstractBuilder implements AppenderBuilder {
   private static final String DEFAULT_DATE_PATTERN = ".yyyy-MM-dd";
   private static final String DATE_PATTERN_PARAM = "DatePattern";
   private static final Logger LOGGER = StatusLogger.getLogger();

   public DailyRollingFileAppenderBuilder() {
   }

   public DailyRollingFileAppenderBuilder(final String prefix, final Properties props) {
      super(prefix, props);
   }

   @Override
   public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
      String name = this.getNameAttribute(appenderElement);
      AtomicReference<Layout> layout = new AtomicReference<>();
      AtomicReference<Filter> filter = new AtomicReference<>();
      AtomicReference<String> fileName = new AtomicReference<>();
      AtomicReference<String> level = new AtomicReference<>();
      AtomicBoolean immediateFlush = new AtomicBoolean(true);
      AtomicBoolean append = new AtomicBoolean(true);
      AtomicBoolean bufferedIo = new AtomicBoolean();
      AtomicInteger bufferSize = new AtomicInteger(8192);
      AtomicReference<String> datePattern = new AtomicReference<>(".yyyy-MM-dd");
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
                  case "Threshold":
                     this.set("Threshold", currentElement, level);
                     break;
                  case "DatePattern":
                     this.set("DatePattern", currentElement, datePattern);
                     break;
                  case "ImmediateFlush":
                     this.set("ImmediateFlush", currentElement, immediateFlush);
               }
         }
      });
      return this.createAppender(
         name,
         layout.get(),
         filter.get(),
         fileName.get(),
         append.get(),
         immediateFlush.get(),
         level.get(),
         bufferedIo.get(),
         bufferSize.get(),
         datePattern.get(),
         config,
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
      String datePattern = this.getProperty("DatePattern", ".yyyy-MM-dd");
      return this.createAppender(
         name,
         layout,
         filter,
         fileName,
         append,
         immediateFlush,
         level,
         bufferedIo,
         bufferSize,
         datePattern,
         configuration,
         (Clock)configuration.getComponent(Clock.KEY)
      );
   }

   private <T extends Log4j1Configuration> Appender createAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final String fileName,
      final boolean append,
      boolean immediateFlush,
      final String level,
      final boolean bufferedIo,
      final int bufferSize,
      final String datePattern,
      final T configuration,
      final Clock clock
   ) {
      org.apache.logging.log4j.core.Layout fileLayout = LayoutAdapter.adapt(layout);
      if (bufferedIo) {
         immediateFlush = false;
      }

      org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
      if (fileName == null) {
         LOGGER.error("Unable to create DailyRollingFileAppender, no file name provided");
         return null;
      } else {
         String filePattern = fileName + "%d{" + datePattern + "}";
         TriggeringPolicy timePolicy = TimeBasedTriggeringPolicy.newBuilder().setClock(clock).setModulate(true).build();
         TriggeringPolicy policy = CompositeTriggeringPolicy.createPolicy(new TriggeringPolicy[]{timePolicy});
         RolloverStrategy strategy = DefaultRolloverStrategy.newBuilder().setConfig(configuration).setMax(Integer.toString(Integer.MAX_VALUE)).build();
         return AppenderWrapper.adapt(
            ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)RollingFileAppender.newBuilder().setName(name))
                                 .setConfiguration(configuration))
                              .setLayout(fileLayout))
                           .setFilter(fileFilter))
                        .setFileName(fileName)
                        .setAppend(append)
                        .setBufferedIo(bufferedIo))
                     .setBufferSize(bufferSize))
                  .setImmediateFlush(immediateFlush))
               .setFilePattern(filePattern)
               .setPolicy(policy)
               .setStrategy(strategy)
               .build()
         );
      }
   }
}
