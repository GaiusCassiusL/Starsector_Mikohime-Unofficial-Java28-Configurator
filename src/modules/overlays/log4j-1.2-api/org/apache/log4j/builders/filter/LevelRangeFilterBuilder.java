package org.apache.log4j.builders.filter;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.filter.LevelRangeFilter;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.varia.LevelRangeFilter")
public class LevelRangeFilterBuilder extends AbstractBuilder<Filter> implements FilterBuilder {
   private static final String LEVEL_MAX = "LevelMax";
   private static final String LEVEL_MIN = "LevelMin";
   private static final String ACCEPT_ON_MATCH = "AcceptOnMatch";

   public LevelRangeFilterBuilder() {
   }

   public LevelRangeFilterBuilder(final String prefix, final Properties props) {
      super(prefix, props);
   }

   public Filter parse(final Element filterElement, final XmlConfiguration config) {
      AtomicReference<String> levelMax = new AtomicReference<>();
      AtomicReference<String> levelMin = new AtomicReference<>();
      AtomicBoolean acceptOnMatch = new AtomicBoolean();
      XmlConfiguration.forEachElement(filterElement.getElementsByTagName("param"), currentElement -> {
         if (currentElement.getTagName().equals("param")) {
            switch (this.getNameAttributeKey(currentElement)) {
               case "LevelMax":
                  levelMax.set(this.getValueAttribute(currentElement));
                  break;
               case "LevelMin":
                  levelMax.set(this.getValueAttribute(currentElement));
                  break;
               case "AcceptOnMatch":
                  acceptOnMatch.set(this.getBooleanValueAttribute(currentElement));
            }
         }
      });
      return this.createFilter(levelMax.get(), levelMin.get(), acceptOnMatch.get());
   }

   public Filter parse(final PropertiesConfiguration config) {
      String levelMax = this.getProperty("LevelMax");
      String levelMin = this.getProperty("LevelMin");
      boolean acceptOnMatch = this.getBooleanProperty("AcceptOnMatch");
      return this.createFilter(levelMax, levelMin, acceptOnMatch);
   }

   private Filter createFilter(final String levelMax, final String levelMin, final boolean acceptOnMatch) {
      Level max = Level.FATAL;
      Level min = Level.TRACE;
      if (levelMax != null) {
         max = OptionConverter.toLevel(levelMax, org.apache.log4j.Level.FATAL).getVersion2Level();
      }

      if (levelMin != null) {
         min = OptionConverter.toLevel(levelMin, org.apache.log4j.Level.DEBUG).getVersion2Level();
      }

      Result onMatch = acceptOnMatch ? Result.ACCEPT : Result.NEUTRAL;
      return FilterWrapper.adapt(LevelRangeFilter.createFilter(min, max, onMatch, Result.DENY));
   }
}
