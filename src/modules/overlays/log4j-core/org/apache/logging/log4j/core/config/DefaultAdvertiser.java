package org.apache.logging.log4j.core.config;

import java.util.Map;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;

@Configurable(elementType = "advertiser")
@Plugin("default")
public class DefaultAdvertiser implements Advertiser {
   @Override
   public Object advertise(final Map<String, String> properties) {
      return null;
   }

   @Override
   public void unadvertise(final Object advertisedObject) {
   }
}
