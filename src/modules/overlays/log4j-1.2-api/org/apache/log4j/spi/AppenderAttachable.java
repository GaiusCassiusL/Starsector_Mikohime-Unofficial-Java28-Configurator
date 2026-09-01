package org.apache.log4j.spi;

import java.util.Enumeration;
import org.apache.log4j.Appender;

public interface AppenderAttachable {
   void addAppender(Appender newAppender);

   Enumeration<Appender> getAllAppenders();

   Appender getAppender(String name);

   boolean isAttached(Appender appender);

   void removeAllAppenders();

   void removeAppender(Appender appender);

   void removeAppender(String name);
}
