package org.apache.log4j.spi;

public class DefaultRepositorySelector implements RepositorySelector {
   final LoggerRepository repository;

   public DefaultRepositorySelector(final LoggerRepository repository) {
      this.repository = repository;
   }

   @Override
   public LoggerRepository getLoggerRepository() {
      return this.repository;
   }
}
