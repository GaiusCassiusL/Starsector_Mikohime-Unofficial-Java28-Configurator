package org.apache.logging.log4j.core.config;

import org.apache.logging.log4j.core.LoggerContext;

public interface LoggerContextAware {
   void setLoggerContext(LoggerContext loggerContext);
}
