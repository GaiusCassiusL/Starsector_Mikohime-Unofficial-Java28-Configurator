package org.apache.logging.log4j.core.config.arbiters;

public interface Arbiter {
   String ELEMENT_TYPE = "Arbiter";

   boolean isCondition();
}
