package org.apache.log4j.varia;

import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class DenyAllFilter extends Filter {
   @Override
   public int decide(final LoggingEvent event) {
      return -1;
   }

   @Deprecated
   public String[] getOptionStrings() {
      return null;
   }

   @Deprecated
   public void setOption(final String key, final String value) {
   }
}
