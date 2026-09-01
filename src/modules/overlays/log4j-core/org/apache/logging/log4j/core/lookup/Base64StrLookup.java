package org.apache.logging.log4j.core.lookup;

import java.util.Base64;
import org.apache.logging.log4j.core.LogEvent;

public class Base64StrLookup extends AbstractLookup {
   @Override
   public String lookup(final LogEvent event, final String key) {
      return new String(Base64.getDecoder().decode(key));
   }
}
