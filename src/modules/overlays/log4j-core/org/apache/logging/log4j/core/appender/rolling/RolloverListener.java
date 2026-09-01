package org.apache.logging.log4j.core.appender.rolling;

public interface RolloverListener {
   void rolloverTriggered(String fileName);

   void rolloverComplete(String fileName);
}
