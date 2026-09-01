package org.apache.logging.log4j.core.pattern;

import java.util.TimeZone;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("FileConverter")
@Plugin("FileDatePatternConverter")
@ConverterKeys({"d", "date"})
@PerformanceSensitive("allocation")
public final class FileDatePatternConverter implements ArrayPatternConverter {
   private final DatePatternConverter delegate;

   private FileDatePatternConverter(final String... options) {
      this.delegate = DatePatternConverter.newInstance(options);
   }

   public static FileDatePatternConverter newInstance(final String[] options) {
      return options != null && options.length != 0 ? new FileDatePatternConverter(options) : new FileDatePatternConverter("yyyy-MM-dd");
   }

   @Override
   public void format(final Object obj, final StringBuilder toAppendTo) {
      this.delegate.format(obj, toAppendTo);
   }

   @Override
   public String getName() {
      return this.delegate.getName();
   }

   @Override
   public String getStyleClass(final Object e) {
      return this.delegate.getStyleClass(e);
   }

   @Override
   public void format(final StringBuilder toAppendTo, final Object... objects) {
      this.delegate.format(toAppendTo, objects);
   }

   public String getPattern() {
      return this.delegate.getPattern();
   }

   public TimeZone getTimeZone() {
      return this.delegate.getTimeZone();
   }
}
