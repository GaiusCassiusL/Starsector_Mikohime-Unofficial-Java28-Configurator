package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("Converter")
@Plugin("ClassNamePatternConverter")
@ConverterKeys({"C", "class"})
public final class ClassNamePatternConverter extends NamePatternConverter {
   private static final String NA = "?";

   private ClassNamePatternConverter(final String[] options) {
      super("Class Name", "class name", options);
   }

   public static ClassNamePatternConverter newInstance(final String[] options) {
      return new ClassNamePatternConverter(options);
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      StackTraceElement element = event.getSource();
      if (element == null) {
         toAppendTo.append("?");
      } else {
         this.abbreviate(element.getClassName(), toAppendTo);
      }
   }

   @Override
   public boolean requiresLocation() {
      return true;
   }
}
