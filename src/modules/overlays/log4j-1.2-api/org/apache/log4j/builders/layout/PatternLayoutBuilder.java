package org.apache.log4j.builders.layout;

import java.util.Properties;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAliases;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.PatternLayout")
@PluginAliases("org.apache.log4j.EnhancedPatternLayout")
public class PatternLayoutBuilder extends AbstractBuilder<Layout> implements LayoutBuilder {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final String PATTERN = "ConversionPattern";

   public PatternLayoutBuilder() {
   }

   public PatternLayoutBuilder(final String prefix, final Properties props) {
      super(prefix, props);
   }

   public Layout parse(final Element layoutElement, final XmlConfiguration config) {
      NodeList params = layoutElement.getElementsByTagName("param");
      int length = params.getLength();
      String pattern = null;

      for (int index = 0; index < length; index++) {
         Node currentNode = params.item(index);
         if (currentNode.getNodeType() == 1) {
            Element currentElement = (Element)currentNode;
            if (currentElement.getTagName().equals("param") && "ConversionPattern".equalsIgnoreCase(currentElement.getAttribute("name"))) {
               pattern = currentElement.getAttribute("value");
               break;
            }
         }
      }

      return this.createLayout(pattern, config);
   }

   public Layout parse(final PropertiesConfiguration config) {
      String pattern = this.getProperty("ConversionPattern");
      return this.createLayout(pattern, config);
   }

   Layout createLayout(String pattern, final Log4j1Configuration config) {
      if (pattern == null) {
         LOGGER.info("No pattern provided for pattern layout, using default pattern");
         pattern = "%m%n";
      }

      return LayoutWrapper.adapt(
         PatternLayout.newBuilder()
            .setPattern(
               pattern.replaceAll("%([-\\.\\d]*)p(?!\\w)", "%$1v1Level")
                  .replaceAll("%([-\\.\\d]*)x(?!\\w)", "%$1ndc")
                  .replaceAll("%([-\\.\\d]*)X(?!\\w)", "%$1properties")
            )
            .setConfiguration(config)
            .build()
      );
   }
}
