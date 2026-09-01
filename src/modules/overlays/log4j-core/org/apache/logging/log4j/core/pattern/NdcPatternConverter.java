package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("NdcPatternConverter")
@ConverterKeys({"x", "NDC"})
public final class NdcPatternConverter extends LogEventPatternConverter {
   private static final NdcPatternConverter INSTANCE = new NdcPatternConverter();

   private NdcPatternConverter() {
      super("NDC", "ndc");
   }

   public static NdcPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   @PerformanceSensitive("allocation")
   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      toAppendTo.append(event.getContextStack());
   }

   @Override
   public String emptyVariableOutput() {
      return "[]";
   }
}
