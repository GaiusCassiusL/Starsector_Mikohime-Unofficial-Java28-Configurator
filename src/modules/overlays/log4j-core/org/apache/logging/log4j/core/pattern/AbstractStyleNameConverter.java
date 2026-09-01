package org.apache.logging.log4j.core.pattern;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.plugins.Namespace;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.util.PerformanceSensitive;

public abstract class AbstractStyleNameConverter extends LogEventPatternConverter {
   private final List<PatternFormatter> formatters;
   private final String style;

   protected AbstractStyleNameConverter(final String name, final List<PatternFormatter> formatters, final String styling) {
      super(name, "style");
      this.formatters = formatters;
      this.style = styling;
   }

   protected static <T extends AbstractStyleNameConverter> T newInstance(
      final Class<T> asnConverterClass, final String name, final Configuration config, final String[] options
   ) {
      List<PatternFormatter> formatters = toPatternFormatterList(config, options);
      if (formatters == null) {
         return null;
      }

      try {
         Constructor<T> constructor = asnConverterClass.getConstructor(List.class, String.class);
         return constructor.newInstance(formatters, AnsiEscape.createSequence(name));
      } catch (SecurityException | NoSuchMethodException | IllegalArgumentException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
         LOGGER.error(e.toString(), e);
         return null;
      }
   }

   private static List<PatternFormatter> toPatternFormatterList(final Configuration config, final String[] options) {
      if (options.length != 0 && options[0] != null) {
         PatternParser parser = PatternLayout.createPatternParser(config);
         if (parser == null) {
            LOGGER.error("No PatternParser created for config=" + config + ", options=" + Arrays.toString(options));
            return null;
         } else {
            return parser.parse(options[0]);
         }
      } else {
         LOGGER.error("No pattern supplied on style for config=" + config);
         return null;
      }
   }

   @PerformanceSensitive("allocation")
   @Override
   public void format(final LogEvent event, final StringBuilder toAppendTo) {
      int start = toAppendTo.length();

      for (int i = 0; i < this.formatters.size(); i++) {
         PatternFormatter formatter = this.formatters.get(i);
         formatter.format(event, toAppendTo);
      }

      if (toAppendTo.length() > start) {
         toAppendTo.insert(start, this.style);
         toAppendTo.append(AnsiEscape.getDefaultStyle());
      }
   }

   @Namespace("Converter")
   @Plugin("black")
   @ConverterKeys("black")
   public static final class Black extends AbstractStyleNameConverter {
      protected static final String NAME = "black";

      public Black(final List<PatternFormatter> formatters, final String styling) {
         super("black", formatters, styling);
      }

      public static AbstractStyleNameConverter.Black newInstance(final Configuration config, final String[] options) {
         return newInstance(AbstractStyleNameConverter.Black.class, "black", config, options);
      }
   }

   @Namespace("Converter")
   @Plugin("blue")
   @ConverterKeys("blue")
   public static final class Blue extends AbstractStyleNameConverter {
      protected static final String NAME = "blue";

      public Blue(final List<PatternFormatter> formatters, final String styling) {
         super("blue", formatters, styling);
      }

      public static AbstractStyleNameConverter.Blue newInstance(final Configuration config, final String[] options) {
         return newInstance(AbstractStyleNameConverter.Blue.class, "blue", config, options);
      }
   }

   @Namespace("Converter")
   @Plugin("cyan")
   @ConverterKeys("cyan")
   public static final class Cyan extends AbstractStyleNameConverter {
      protected static final String NAME = "cyan";

      public Cyan(final List<PatternFormatter> formatters, final String styling) {
         super("cyan", formatters, styling);
      }

      public static AbstractStyleNameConverter.Cyan newInstance(final Configuration config, final String[] options) {
         return newInstance(AbstractStyleNameConverter.Cyan.class, "cyan", config, options);
      }
   }

   @Namespace("Converter")
   @Plugin("green")
   @ConverterKeys("green")
   public static final class Green extends AbstractStyleNameConverter {
      protected static final String NAME = "green";

      public Green(final List<PatternFormatter> formatters, final String styling) {
         super("green", formatters, styling);
      }

      public static AbstractStyleNameConverter.Green newInstance(final Configuration config, final String[] options) {
         return newInstance(AbstractStyleNameConverter.Green.class, "green", config, options);
      }
   }

   @Namespace("Converter")
   @Plugin("magenta")
   @ConverterKeys("magenta")
   public static final class Magenta extends AbstractStyleNameConverter {
      protected static final String NAME = "magenta";

      public Magenta(final List<PatternFormatter> formatters, final String styling) {
         super("magenta", formatters, styling);
      }

      public static AbstractStyleNameConverter.Magenta newInstance(final Configuration config, final String[] options) {
         return newInstance(AbstractStyleNameConverter.Magenta.class, "magenta", config, options);
      }
   }

   @Namespace("Converter")
   @Plugin("red")
   @ConverterKeys("red")
   public static final class Red extends AbstractStyleNameConverter {
      protected static final String NAME = "red";

      public Red(final List<PatternFormatter> formatters, final String styling) {
         super("red", formatters, styling);
      }

      public static AbstractStyleNameConverter.Red newInstance(final Configuration config, final String[] options) {
         return newInstance(AbstractStyleNameConverter.Red.class, "red", config, options);
      }
   }

   @Namespace("Converter")
   @Plugin("white")
   @ConverterKeys("white")
   public static final class White extends AbstractStyleNameConverter {
      protected static final String NAME = "white";

      public White(final List<PatternFormatter> formatters, final String styling) {
         super("white", formatters, styling);
      }

      public static AbstractStyleNameConverter.White newInstance(final Configuration config, final String[] options) {
         return newInstance(AbstractStyleNameConverter.White.class, "white", config, options);
      }
   }

   @Namespace("Converter")
   @Plugin("yellow")
   @ConverterKeys("yellow")
   public static final class Yellow extends AbstractStyleNameConverter {
      protected static final String NAME = "yellow";

      public Yellow(final List<PatternFormatter> formatters, final String styling) {
         super("yellow", formatters, styling);
      }

      public static AbstractStyleNameConverter.Yellow newInstance(final Configuration config, final String[] options) {
         return newInstance(AbstractStyleNameConverter.Yellow.class, "yellow", config, options);
      }
   }
}
