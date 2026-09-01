package org.apache.logging.log4j.core.pattern;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.util.ProcessIdUtil;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;

@Namespace("Converter")
@Plugin("ProcessIdPatternConverter")
@ConverterKeys({"pid", "processId"})
public final class ProcessIdPatternConverter extends LogEventPatternConverter {
   private static final String DEFAULT_DEFAULT_VALUE = "???";
   private final String pid;

   private ProcessIdPatternConverter(final String... options) {
      super("Process ID", "pid");
      String defaultValue = options.length > 0 ? options[0] : "???";
      String discoveredPid = ProcessIdUtil.getProcessId();
      this.pid = discoveredPid.equals("-") ? defaultValue : discoveredPid;
   }

   public String getProcessId() {
      return this.pid;
   }

   public static void main(final String[] args) {
      System.out.println((new ProcessIdPatternConverter()).pid);
   }

   public static ProcessIdPatternConverter newInstance(final String[] options) {
      return new ProcessIdPatternConverter(options);
   }

   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      toAppendTo.append(this.pid);
   }
}
