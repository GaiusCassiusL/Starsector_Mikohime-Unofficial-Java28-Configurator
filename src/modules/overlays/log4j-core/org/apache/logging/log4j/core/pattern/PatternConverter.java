package org.apache.logging.log4j.core.pattern;

public interface PatternConverter {
   String CATEGORY = "Converter";

   void format(Object obj, StringBuilder toAppendTo);

   String getName();

   String getStyleClass(Object e);
}
