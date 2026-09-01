package org.apache.log4j.builders.appender;

import java.util.Properties;
import org.apache.log4j.Appender;
import org.apache.log4j.builders.Builder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.xml.XmlConfiguration;
import org.w3c.dom.Element;

public interface AppenderBuilder<T extends Appender> extends Builder<T> {
   Appender parseAppender(Element element, XmlConfiguration configuration);

   Appender parseAppender(String name, String appenderPrefix, String layoutPrefix, String filterPrefix, Properties props, PropertiesConfiguration configuration);
}
