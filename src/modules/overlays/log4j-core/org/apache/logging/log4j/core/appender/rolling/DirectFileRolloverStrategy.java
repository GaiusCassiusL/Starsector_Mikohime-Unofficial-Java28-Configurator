package org.apache.logging.log4j.core.appender.rolling;

public interface DirectFileRolloverStrategy {
   String getCurrentFileName(final RollingFileManager manager);

   void clearCurrentFileName();
}
