package com.thoughtworks.xstream.converters.extended;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

class DurationConverter$1 {
   DatatypeFactory getFactory() {
      try {
         return DatatypeFactory.newInstance();
      } catch (DatatypeConfigurationException e) {
         return null;
      }
   }
}
