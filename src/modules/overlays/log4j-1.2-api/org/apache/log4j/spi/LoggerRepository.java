package org.apache.log4j.spi;

import java.util.Enumeration;
import org.apache.log4j.Appender;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public interface LoggerRepository {
   void addHierarchyEventListener(HierarchyEventListener listener);

   boolean isDisabled(int level);

   void setThreshold(Level level);

   void setThreshold(String val);

   void emitNoAppenderWarning(Category cat);

   Level getThreshold();

   Logger getLogger(String name);

   Logger getLogger(String name, LoggerFactory factory);

   Logger getRootLogger();

   Logger exists(String name);

   void shutdown();

   Enumeration getCurrentLoggers();

   Enumeration getCurrentCategories();

   void fireAddAppenderEvent(Category logger, Appender appender);

   void resetConfiguration();
}
