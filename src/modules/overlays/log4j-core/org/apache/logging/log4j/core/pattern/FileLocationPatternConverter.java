package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("Converter")
@Plugin("FileLocationPatternConverter")
@ConverterKeys({"F", "file"})
public final class FileLocationPatternConverter extends LogEventPatternConverter {
   private static final FileLocationPatternConverter INSTANCE = new FileLocationPatternConverter();

   private FileLocationPatternConverter() {
      super("File Location", "file");
   }

   public static FileLocationPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   @Override
   public void format(final LogEvent event, final StringBuilder output) {
      StackTraceElement element = event.getSource();
      if (element != null) {
         output.append(element.getFileName());
      }
   }
}
