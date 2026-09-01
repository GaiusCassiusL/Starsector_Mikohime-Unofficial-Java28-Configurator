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
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.appender.FileAppender.Builder;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.FileAppender")
public class FileAppenderBuilder extends AbstractBuilder implements AppenderBuilder {
   private static final Logger LOGGER = StatusLogger.getLogger();

   public FileAppenderBuilder() {
   }

   public FileAppenderBuilder(final String prefix, final Properties props) {
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
                  case "ImmediateFlush":
                     this.set("ImmediateFlush", currentElement, immediateFlush);
               }
         }
      });
      return this.createAppender(
         name, config, layout.get(), filter.get(), fileName.get(), level.get(), immediateFlush.get(), append.get(), bufferedIo.get(), bufferSize.get()
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
      String level = this.getProperty("Threshold");
      String fileName = this.getProperty("File");
      boolean append = this.getBooleanProperty("Append", true);
      boolean immediateFlush = this.getBooleanProperty("ImmediateFlush", true);
      boolean bufferedIo = this.getBooleanProperty("BufferedIO", false);
      int bufferSize = Integer.parseInt(this.getProperty("BufferSize", "8192"));
      return this.createAppender(name, configuration, layout, filter, fileName, level, immediateFlush, append, bufferedIo, bufferSize);
   }

   private Appender createAppender(
      final String name,
      final Log4j1Configuration configuration,
      final Layout layout,
      final Filter filter,
      final String fileName,
      final String level,
      boolean immediateFlush,
      final boolean append,
      final boolean bufferedIo,
      final int bufferSize
   ) {
      org.apache.logging.log4j.core.Layout fileLayout = LayoutAdapter.adapt(layout);
      if (bufferedIo) {
         immediateFlush = false;
      }

      org.apache.logging.log4j.core.Filter fileFilter = buildFilters(level, filter);
      if (fileName == null) {
         LOGGER.error("Unable to create FileAppender, no file name provided");
         return null;
      } else {
         return AppenderWrapper.adapt(
            ((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)((Builder)FileAppender.newBuilder().setName(name)).setConfiguration(configuration))
                              .setLayout(fileLayout))
                           .setFilter(fileFilter))
                        .setFileName(fileName)
                        .setImmediateFlush(immediateFlush))
                     .setAppend(append)
                     .setBufferedIo(bufferedIo))
                  .setBufferSize(bufferSize))
               .build()
         );
      }
   }
}
