package org.apache.logging.log4j.core.appender;

public interface ManagerFactory<M, T> {
   M createManager(String name, T data);
}
