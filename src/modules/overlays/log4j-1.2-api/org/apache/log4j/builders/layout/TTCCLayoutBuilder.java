package org.apache.log4j.builders.layout;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.TTCCLayout")
public class TTCCLayoutBuilder extends AbstractBuilder<Layout> implements LayoutBuilder {
   private static final String THREAD_PRINTING_PARAM = "ThreadPrinting";
   private static final String CATEGORY_PREFIXING_PARAM = "CategoryPrefixing";
   private static final String CONTEXT_PRINTING_PARAM = "ContextPrinting";
   private static final String DATE_FORMAT_PARAM = "DateFormat";
   private static final String TIMEZONE_FORMAT = "TimeZone";

   public TTCCLayoutBuilder() {
   }

   public TTCCLayoutBuilder(final String prefix, final Properties props) {
      super(prefix, props);
   }

   public Layout parse(final Element layoutElement, final XmlConfiguration config) {
      AtomicBoolean threadPrinting = new AtomicBoolean(Boolean.TRUE);
      AtomicBoolean categoryPrefixing = new AtomicBoolean(Boolean.TRUE);
      AtomicBoolean contextPrinting = new AtomicBoolean(Boolean.TRUE);
      AtomicReference<String> dateFormat = new AtomicReference<>("RELATIVE");
      AtomicReference<String> timezone = new AtomicReference<>();
      XmlConfiguration.forEachElement(layoutElement.getElementsByTagName("param"), currentElement -> {
         if (currentElement.getTagName().equals("param")) {
            switch (this.getNameAttributeKey(currentElement)) {
               case "ThreadPrinting":
                  threadPrinting.set(this.getBooleanValueAttribute(currentElement));
                  break;
               case "CategoryPrefixing":
                  categoryPrefixing.set(this.getBooleanValueAttribute(currentElement));
                  break;
               case "ContextPrinting":
                  contextPrinting.set(this.getBooleanValueAttribute(currentElement));
                  break;
               case "DateFormat":
                  dateFormat.set(this.getValueAttribute(currentElement));
                  break;
               case "TimeZone":
                  timezone.set(this.getValueAttribute(currentElement));
            }
         }
      });
      return this.createLayout(threadPrinting.get(), categoryPrefixing.get(), contextPrinting.get(), dateFormat.get(), timezone.get(), config);
   }

   public Layout parse(final PropertiesConfiguration config) {
      boolean threadPrinting = this.getBooleanProperty("ThreadPrinting", true);
      boolean categoryPrefixing = this.getBooleanProperty("CategoryPrefixing", true);
      boolean contextPrinting = this.getBooleanProperty("ContextPrinting", true);
      String dateFormat = this.getProperty("DateFormat", "RELATIVE");
      String timezone = this.getProperty("TimeZone");
      return this.createLayout(threadPrinting, categoryPrefixing, contextPrinting, dateFormat, timezone, config);
   }

   private Layout createLayout(
      final boolean threadPrinting,
      final boolean categoryPrefixing,
      final boolean contextPrinting,
      final String dateFormat,
      final String timezone,
      final Log4j1Configuration config
   ) {
      StringBuilder sb = new StringBuilder();
      if (dateFormat != null) {
         if ("RELATIVE".equalsIgnoreCase(dateFormat)) {
            sb.append("%r ");
         } else if (!"NULL".equalsIgnoreCase(dateFormat)) {
            sb.append("%d{").append(dateFormat).append("}");
            if (timezone != null) {
               sb.append("{").append(timezone).append("}");
            }

            sb.append(" ");
         }
      }

      if (threadPrinting) {
         sb.append("[%t] ");
      }

      sb.append("%p ");
      if (categoryPrefixing) {
         sb.append("%c ");
      }

      if (contextPrinting) {
         sb.append("%notEmpty{%ndc }");
      }

      sb.append("- %m%n");
      return LayoutWrapper.adapt(PatternLayout.newBuilder().setPattern(sb.toString()).setConfiguration(config).build());
   }
}
