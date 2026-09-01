package org.apache.log4j.builders.layout;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.LayoutWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.layout.Log4j1XmlLayout;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.xml.XMLLayout")
public class XmlLayoutBuilder extends AbstractBuilder<Layout> implements LayoutBuilder {
   private static final String LOCATION_INFO = "LocationInfo";
   private static final String PROPERTIES = "Properties";

   public XmlLayoutBuilder() {
   }

   public XmlLayoutBuilder(final String prefix, final Properties props) {
      super(prefix, props);
   }

   public Layout parse(final Element layoutElement, final XmlConfiguration config) {
      AtomicBoolean properties = new AtomicBoolean();
      AtomicBoolean locationInfo = new AtomicBoolean();
      XmlConfiguration.forEachElement(layoutElement.getElementsByTagName("param"), currentElement -> {
         if ("Properties".equalsIgnoreCase(currentElement.getAttribute("name"))) {
            properties.set(this.getBooleanValueAttribute(currentElement));
         } else if ("LocationInfo".equalsIgnoreCase(currentElement.getAttribute("name"))) {
            locationInfo.set(this.getBooleanValueAttribute(currentElement));
         }
      });
      return this.createLayout(properties.get(), locationInfo.get());
   }

   public Layout parse(final PropertiesConfiguration config) {
      boolean properties = this.getBooleanProperty("Properties");
      boolean locationInfo = this.getBooleanProperty("LocationInfo");
      return this.createLayout(properties, locationInfo);
   }

   private Layout createLayout(final boolean properties, final boolean locationInfo) {
      return LayoutWrapper.adapt(Log4j1XmlLayout.createLayout(locationInfo, properties));
   }
}
