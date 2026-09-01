package org.apache.logging.log4j.core;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.logging.log4j.core.layout.Encoder;

public interface Layout extends Encoder<LogEvent> {
   String ELEMENT_TYPE = "layout";

   byte[] getFooter();

   byte[] getHeader();

   byte[] toByteArray(LogEvent event);

   default Charset getCharset() {
      return StandardCharsets.UTF_8;
   }

   String toSerializable(LogEvent event);

   String getContentType();

   Map<String, String> getContentFormat();

   default boolean requiresLocation() {
      return false;
   }
}
