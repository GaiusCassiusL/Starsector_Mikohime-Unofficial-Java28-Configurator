package org.apache.log4j.builders;

import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.w3c.dom.Element;

public interface Parser<T> extends Builder<T> {
   T parse(Element element, XmlConfiguration config);

   T parse(PropertiesConfiguration config);
}
