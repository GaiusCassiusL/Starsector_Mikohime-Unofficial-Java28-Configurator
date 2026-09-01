package org.apache.logging.log4j.core.pattern;

import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

@Namespace("Converter")
@Plugin("SequenceNumberPatternConverter")
@ConverterKeys({"sn", "sequenceNumber"})
@PerformanceSensitive("allocation")
public final class SequenceNumberPatternConverter extends LogEventPatternConverter {
   private static final AtomicLong SEQUENCE = new AtomicLong();
   private static final SequenceNumberPatternConverter INSTANCE = new SequenceNumberPatternConverter();

   private SequenceNumberPatternConverter() {
      super("Sequence Number", "sn");
   }

   public static SequenceNumberPatternConverter newInstance(final String[] options) {
      return INSTANCE;
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      toAppendTo.append(SEQUENCE.incrementAndGet());
   }
}
