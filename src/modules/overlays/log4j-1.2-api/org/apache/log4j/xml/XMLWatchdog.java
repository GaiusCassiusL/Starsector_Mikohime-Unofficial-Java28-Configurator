package org.apache.log4j.xml;

import org.apache.log4j.LogManager;
import org.apache.log4j.helpers.FileWatchdog;

class XMLWatchdog extends FileWatchdog {
   XMLWatchdog(final String filename) {
      super(filename);
   }

   @Override
   public void doOnChange() {
      new DOMConfigurator().doConfigure(this.filename, LogManager.getLoggerRepository());
   }
}
