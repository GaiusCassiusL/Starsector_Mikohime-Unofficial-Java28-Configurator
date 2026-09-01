package org.apache.logging.log4j.core.jmx;

public interface LoggerConfigAdminMBean {
   String PATTERN = "org.apache.logging.log4j2:type=%s,component=Loggers,name=%s";

   String getName();

   String getLevel();

   void setLevel(String level);

   boolean isAdditive();

   void setAdditive(boolean additive);

   boolean isIncludeLocation();

   String getFilter();

   String[] getAppenderRefs();
}
