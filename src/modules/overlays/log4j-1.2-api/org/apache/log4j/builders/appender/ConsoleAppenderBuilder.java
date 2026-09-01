package org.apache.log4j.builders.appender;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Builder;
import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.ConsoleAppender")
public class ConsoleAppenderBuilder extends AbstractBuilder implements AppenderBuilder {
   private static final String SYSTEM_OUT = "System.out";
   private static final String SYSTEM_ERR = "System.err";
   private static final String TARGET_PARAM = "Target";
   private static final String FOLLOW_PARAM = "Follow";
   private static final Logger LOGGER = StatusLogger.getLogger();

   public ConsoleAppenderBuilder() {
   }

   public ConsoleAppenderBuilder(final String prefix, final Properties props) {
      super(prefix, props);
   }

   @Override
   public Appender parseAppender(final Element appenderElement, final XmlConfiguration config) {
      String name = this.getNameAttribute(appenderElement);
      AtomicReference<String> target = new AtomicReference<>("System.out");
      AtomicReference<Layout> layout = new AtomicReference<>();
      AtomicReference<Filter> filter = new AtomicReference<>();
      AtomicReference<String> level = new AtomicReference<>();
      AtomicBoolean follow = new AtomicBoolean();
      AtomicBoolean immediateFlush = new AtomicBoolean(true);
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
                  case "Target":
                     String value = this.getValueAttribute(currentElement);
                     if (value == null) {
                        LOGGER.warn("No value supplied for target parameter. Defaulting to System.out");
                     } else {
                        switch (value) {
                           case "System.out":
                              target.set("System.out");
                              return;
                           case "System.err":
                              target.set("System.err");
                              return;
                           default:
                              LOGGER.warn("Invalid value \"{}\" for target parameter. Using default of {}", value, "System.out");
                        }
                     }
                     break;
                  case "Threshold":
                     this.set("Threshold", currentElement, level);
                     break;
                  case "Follow":
                     this.set("Follow", currentElement, follow);
                     break;
                  case "ImmediateFlush":
                     this.set("ImmediateFlush", currentElement, immediateFlush);
               }
         }
      });
      return this.createAppender(name, layout.get(), filter.get(), level.get(), target.get(), immediateFlush.get(), follow.get(), config);
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
      String target = this.getProperty("Target");
      boolean follow = this.getBooleanProperty("Follow");
      boolean immediateFlush = this.getBooleanProperty("ImmediateFlush");
      return this.createAppender(name, layout, filter, level, target, immediateFlush, follow, configuration);
   }

   private <T extends Log4j1Configuration> Appender createAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final String level,
      final String target,
      final boolean immediateFlush,
      final boolean follow,
      final T configuration
   ) {
      org.apache.logging.log4j.core.Layout consoleLayout = LayoutAdapter.adapt(layout);
      org.apache.logging.log4j.core.Filter consoleFilter = buildFilters(level, filter);
      Target consoleTarget = "System.err".equals(target) ? Target.SYSTEM_ERR : Target.SYSTEM_OUT;
      return AppenderWrapper.adapt(
         ((Builder)((Builder)((Builder)((Builder)ConsoleAppender.newBuilder().setName(name))
                     .setTarget(consoleTarget)
                     .setFollow(follow)
                     .setLayout(consoleLayout))
                  .setFilter(consoleFilter))
               .setConfiguration(configuration)
               .setImmediateFlush(immediateFlush))
            .build()
      );
   }
}
