package org.apache.log4j.builders.filter;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.builders.AbstractBuilder;
import org.apache.log4j.config.PropertiesConfiguration;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.xml.XmlConfiguration;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.filter.StringMatchFilter;
import org.apache.logging.log4j.core.filter.StringMatchFilter.Builder;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.status.StatusLogger;
import org.w3c.dom.Element;

@Namespace("Log4j Builder")
@Plugin("org.apache.log4j.varia.StringMatchFilter")
public class StringMatchFilterBuilder extends AbstractBuilder<Filter> implements FilterBuilder {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final String STRING_TO_MATCH = "StringToMatch";
   private static final String ACCEPT_ON_MATCH = "AcceptOnMatch";

   public StringMatchFilterBuilder() {
   }

   public StringMatchFilterBuilder(final String prefix, final Properties props) {
      super(prefix, props);
   }

   public Filter parse(final Element filterElement, final XmlConfiguration config) {
      AtomicBoolean acceptOnMatch = new AtomicBoolean();
      AtomicReference<String> text = new AtomicReference<>();
      XmlConfiguration.forEachElement(filterElement.getElementsByTagName("param"), currentElement -> {
         if (currentElement.getTagName().equals("param")) {
            switch (this.getNameAttributeKey(currentElement)) {
               case "StringToMatch":
                  text.set(this.getValueAttribute(currentElement));
                  break;
               case "AcceptOnMatch":
                  acceptOnMatch.set(this.getBooleanValueAttribute(currentElement));
            }
         }
      });
      return this.createFilter(text.get(), acceptOnMatch.get());
   }

   public Filter parse(final PropertiesConfiguration config) {
      String text = this.getProperty("StringToMatch");
      boolean acceptOnMatch = this.getBooleanProperty("AcceptOnMatch");
      return this.createFilter(text, acceptOnMatch);
   }

   private Filter createFilter(final String text, final boolean acceptOnMatch) {
      if (text == null) {
         LOGGER.error("No text provided for StringMatchFilter");
         return null;
      } else {
         Result onMatch = acceptOnMatch ? Result.ACCEPT : Result.DENY;
         return FilterWrapper.adapt(
            ((Builder)((Builder)StringMatchFilter.newBuilder().setMatchString(text).setOnMatch(onMatch)).setOnMismatch(Result.NEUTRAL)).build()
         );
      }
   }
}
