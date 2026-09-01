package org.apache.log4j.varia;

import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

public class StringMatchFilter extends Filter {
   @Deprecated
   public static final String STRING_TO_MATCH_OPTION = "StringToMatch";
   @Deprecated
   public static final String ACCEPT_ON_MATCH_OPTION = "AcceptOnMatch";
   boolean acceptOnMatch = true;
   String stringToMatch;

   @Override
   public int decide(final LoggingEvent event) {
      String msg = event.getRenderedMessage();
      if (msg != null && this.stringToMatch != null) {
         if (msg.indexOf(this.stringToMatch) == -1) {
            return 0;
         } else {
            return this.acceptOnMatch ? 1 : -1;
         }
      } else {
         return 0;
      }
   }

   public boolean getAcceptOnMatch() {
      return this.acceptOnMatch;
   }

   @Deprecated
   public String[] getOptionStrings() {
      return new String[]{"StringToMatch", "AcceptOnMatch"};
   }

   public String getStringToMatch() {
      return this.stringToMatch;
   }

   public void setAcceptOnMatch(final boolean acceptOnMatch) {
      this.acceptOnMatch = acceptOnMatch;
   }

   @Deprecated
   public void setOption(final String key, final String value) {
      if (key.equalsIgnoreCase("StringToMatch")) {
         this.stringToMatch = value;
      } else if (key.equalsIgnoreCase("AcceptOnMatch")) {
         this.acceptOnMatch = OptionConverter.toBoolean(value, this.acceptOnMatch);
      }
   }

   public void setStringToMatch(final String s) {
      this.stringToMatch = s;
   }
}
