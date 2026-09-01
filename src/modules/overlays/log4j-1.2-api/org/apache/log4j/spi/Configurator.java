package org.apache.log4j.spi;

import java.io.InputStream;
import java.net.URL;

public interface Configurator {
   String INHERITED = "inherited";
   String NULL = "null";

   void doConfigure(InputStream inputStream, final LoggerRepository loggerRepository);

   void doConfigure(URL url, final LoggerRepository loggerRepository);
}
