package org.apache.log4j.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Constants;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;

public class Log4jEntityResolver implements EntityResolver {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final String PUBLIC_ID = "-//APACHE//DTD LOG4J 1.2//EN";

   @Override
   public InputSource resolveEntity(final String publicId, final String systemId) {
      if (!systemId.endsWith("log4j.dtd") && !"-//APACHE//DTD LOG4J 1.2//EN".equals(publicId)) {
         return null;
      }

      Class<?> clazz = this.getClass();
      InputStream in = clazz.getResourceAsStream("/org/apache/log4j/xml/log4j.dtd");
      if (in == null) {
         LOGGER.warn("Could not find [log4j.dtd] using [{}] class loader, parsed without DTD.", clazz.getClassLoader());
         in = new ByteArrayInputStream(Constants.EMPTY_BYTE_ARRAY);
      }

      return new InputSource(in);
   }
}
