package org.apache.log4j.builders.layout;

import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.SimpleLayout")
public class SimpleLayoutBuilder implements LayoutBuilder {
   public Layout parse(final Element layoutElement, final XmlConfiguration config) {
      return new LayoutWrapper(PatternLayout.newBuilder().setPattern("%v1Level - %m%n").setConfiguration(config).build());
   }

   public Layout parse(final PropertiesConfiguration config) {
      return new LayoutWrapper(PatternLayout.newBuilder().setPattern("%v1Level - %m%n").setConfiguration(config).build());
   }
}
