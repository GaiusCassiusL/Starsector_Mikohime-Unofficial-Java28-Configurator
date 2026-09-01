package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.impl.ThrowableProxy;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("Converter")
@Plugin("ExtendedThrowablePatternConverter")
@ConverterKeys({"xEx", "xThrowable", "xException"})
public final class ExtendedThrowablePatternConverter extends ThrowablePatternConverter {
   private ExtendedThrowablePatternConverter(final Configuration config, final String[] options) {
      super("ExtendedThrowable", "throwable", options, config);
   }

   public static ExtendedThrowablePatternConverter newInstance(final Configuration config, final String[] options) {
      return new ExtendedThrowablePatternConverter(config, options);
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      ThrowableProxy proxy = event.getThrownProxy();
      Throwable throwable = event.getThrown();
      if ((throwable != null || proxy != null) && this.options.anyLines()) {
         if (proxy == null) {
            super.format(event, toAppendTo);
            return;
         }

         int len = toAppendTo.length();
         if (len > 0 && !Character.isWhitespace(toAppendTo.charAt(len - 1))) {
            toAppendTo.append(' ');
         }

         proxy.formatExtendedStackTraceTo(
            toAppendTo, this.options.getIgnorePackages(), this.options.getTextRenderer(), this.getSuffix(event), this.options.getSeparator()
         );
      }
   }
}
