package org.apache.log4j.builders.filter;

import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.core.filter.DenyAllFilter;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.varia.DenyAllFilter")
public class DenyAllFilterBuilder implements FilterBuilder {
   public Filter parse(final Element filterElement, final XmlConfiguration config) {
      return new FilterWrapper(DenyAllFilter.newBuilder().build());
   }

   public Filter parse(final PropertiesConfiguration config) {
      return new FilterWrapper(DenyAllFilter.newBuilder().build());
   }
}
