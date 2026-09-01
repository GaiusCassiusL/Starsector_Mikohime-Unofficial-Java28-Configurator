package org.apache.logging.log4j.core.tools.picocli;

import java.io.File;
import java.io.PrintStream;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Time;
import java.text.BreakIterator;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

public class CommandLine {
   public static final String VERSION = "2.0.3";
   private final CommandLine.Tracer tracer = new CommandLine.Tracer();
   private final CommandLine.Interpreter interpreter;
   private String commandName = "<main class>";
   private boolean overwrittenOptionsAllowed = false;
   private boolean unmatchedArgumentsAllowed = false;
   private final List<String> unmatchedArguments = new ArrayList<>();
   private CommandLine parent;
   private boolean usageHelpRequested;
   private boolean versionHelpRequested;
   private final List<String> versionLines = new ArrayList<>();

   public CommandLine(final Object command) {
      this.interpreter = new CommandLine.Interpreter(command);
   }

   public CommandLine addSubcommand(final String name, final Object command) {
      CommandLine commandLine = toCommandLine(command);
      commandLine.parent = this;
      this.interpreter.commands.put(name, commandLine);
      return this;
   }

   public Map<String, CommandLine> getSubcommands() {
      return new LinkedHashMap<>(this.interpreter.commands);
   }

   public CommandLine getParent() {
      return this.parent;
   }

   public <T> T getCommand() {
      return (T)this.interpreter.command;
   }

   public boolean isUsageHelpRequested() {
      return this.usageHelpRequested;
   }

   public boolean isVersionHelpRequested() {
      return this.versionHelpRequested;
   }

   public boolean isOverwrittenOptionsAllowed() {
      return this.overwrittenOptionsAllowed;
   }

   public CommandLine setOverwrittenOptionsAllowed(final boolean newValue) {
      this.overwrittenOptionsAllowed = newValue;

      for (CommandLine command : this.interpreter.commands.values()) {
         command.setOverwrittenOptionsAllowed(newValue);
      }

      return this;
   }

   public boolean isUnmatchedArgumentsAllowed() {
      return this.unmatchedArgumentsAllowed;
   }

   public CommandLine setUnmatchedArgumentsAllowed(final boolean newValue) {
      this.unmatchedArgumentsAllowed = newValue;

      for (CommandLine command : this.interpreter.commands.values()) {
         command.setUnmatchedArgumentsAllowed(newValue);
      }

      return this;
   }

   public List<String> getUnmatchedArguments() {
      return this.unmatchedArguments;
   }

   public static <T> T populateCommand(final T command, final String... args) {
      CommandLine cli = toCommandLine(command);
      cli.parse(args);
      return command;
   }

   public List<CommandLine> parse(final String... args) {
      return this.interpreter.parse(args);
   }

   public static boolean printHelpIfRequested(final List<CommandLine> parsedCommands, final PrintStream out, final CommandLine.Help.Ansi ansi) {
      for (CommandLine parsed : parsedCommands) {
         if (parsed.isUsageHelpRequested()) {
            parsed.usage(out, ansi);
            return true;
         }

         if (parsed.isVersionHelpRequested()) {
            parsed.printVersionHelp(out, ansi);
            return true;
         }
      }

      return false;
   }

   private static Object execute(final CommandLine parsed) {
      Object command = parsed.getCommand();
      if (command instanceof Runnable) {
         try {
            ((Runnable)command).run();
            return null;
         } catch (Exception ex) {
            throw new CommandLine.ExecutionException(parsed, "Error while running command (" + command + ")", ex);
         }
      } else if (command instanceof Callable) {
         try {
            return ((Callable)command).call();
         } catch (Exception ex) {
            throw new CommandLine.ExecutionException(parsed, "Error while calling command (" + command + ")", ex);
         }
      } else {
         throw new CommandLine.ExecutionException(parsed, "Parsed command (" + command + ") is not Runnable or Callable");
      }
   }

   public List<Object> parseWithHandler(final CommandLine.IParseResultHandler handler, final PrintStream out, final String... args) {
      return this.parseWithHandlers(handler, out, CommandLine.Help.Ansi.AUTO, new CommandLine.DefaultExceptionHandler(), args);
   }

   public List<Object> parseWithHandlers(
      final CommandLine.IParseResultHandler handler,
      final PrintStream out,
      final CommandLine.Help.Ansi ansi,
      final CommandLine.IExceptionHandler exceptionHandler,
      final String... args
   ) {
      try {
         List<CommandLine> result = this.parse(args);
         return handler.handleParseResult(result, out, ansi);
      } catch (CommandLine.ParameterException ex) {
         return exceptionHandler.handleException(ex, out, ansi, args);
      }
   }

   public static void usage(final Object command, final PrintStream out) {
      toCommandLine(command).usage(out);
   }

   public static void usage(final Object command, final PrintStream out, final CommandLine.Help.Ansi ansi) {
      toCommandLine(command).usage(out, ansi);
   }

   public static void usage(final Object command, final PrintStream out, final CommandLine.Help.ColorScheme colorScheme) {
      toCommandLine(command).usage(out, colorScheme);
   }

   public void usage(final PrintStream out) {
      this.usage(out, CommandLine.Help.Ansi.AUTO);
   }

   public void usage(final PrintStream out, final CommandLine.Help.Ansi ansi) {
      this.usage(out, CommandLine.Help.defaultColorScheme(ansi));
   }

   public void usage(final PrintStream out, final CommandLine.Help.ColorScheme colorScheme) {
      CommandLine.Help help = new CommandLine.Help(this.interpreter.command, colorScheme).addAllSubcommands(this.getSubcommands());
      if (!"=".equals(this.getSeparator())) {
         help.separator = this.getSeparator();
         help.parameterLabelRenderer = help.createDefaultParamLabelRenderer();
      }

      if (!"<main class>".equals(this.getCommandName())) {
         help.commandName = this.getCommandName();
      }

      StringBuilder sb = new StringBuilder()
         .append(help.headerHeading())
         .append(help.header())
         .append(help.synopsisHeading())
         .append(help.synopsis(help.synopsisHeadingLength()))
         .append(help.descriptionHeading())
         .append(help.description())
         .append(help.parameterListHeading())
         .append(help.parameterList())
         .append(help.optionListHeading())
         .append(help.optionList())
         .append(help.commandListHeading())
         .append(help.commandList())
         .append(help.footerHeading())
         .append(help.footer());
      out.print(sb);
   }

   public void printVersionHelp(final PrintStream out) {
      this.printVersionHelp(out, CommandLine.Help.Ansi.AUTO);
   }

   public void printVersionHelp(final PrintStream out, final CommandLine.Help.Ansi ansi) {
      for (String versionInfo : this.versionLines) {
         out.println(ansi.new Text(versionInfo));
      }
   }

   public void printVersionHelp(final PrintStream out, final CommandLine.Help.Ansi ansi, final Object... params) {
      for (String versionInfo : this.versionLines) {
         out.println(ansi.new Text(String.format(versionInfo, params)));
      }
   }

   public static <C extends Callable<T>, T> T call(final C callable, final PrintStream out, final String... args) {
      return call(callable, out, CommandLine.Help.Ansi.AUTO, args);
   }

   public static <C extends Callable<T>, T> T call(final C callable, final PrintStream out, final CommandLine.Help.Ansi ansi, final String... args) {
      CommandLine cmd = new CommandLine(callable);
      List<Object> results = cmd.parseWithHandlers(new CommandLine.RunLast(), out, ansi, new CommandLine.DefaultExceptionHandler(), args);
      return (T)(results != null && !results.isEmpty() ? results.get(0) : null);
   }

   public static <R extends Runnable> void run(final R runnable, final PrintStream out, final String... args) {
      run(runnable, out, CommandLine.Help.Ansi.AUTO, args);
   }

   public static <R extends Runnable> void run(final R runnable, final PrintStream out, final CommandLine.Help.Ansi ansi, final String... args) {
      CommandLine cmd = new CommandLine(runnable);
      cmd.parseWithHandlers(new CommandLine.RunLast(), out, ansi, new CommandLine.DefaultExceptionHandler(), args);
   }

   public <K> CommandLine registerConverter(final Class<K> cls, final CommandLine.ITypeConverter<K> converter) {
      this.interpreter.converterRegistry.put(CommandLine.Assert.notNull(cls, "class"), CommandLine.Assert.notNull(converter, "converter"));

      for (CommandLine command : this.interpreter.commands.values()) {
         command.registerConverter(cls, converter);
      }

      return this;
   }

   public String getSeparator() {
      return this.interpreter.separator;
   }

   public CommandLine setSeparator(final String separator) {
      this.interpreter.separator = CommandLine.Assert.notNull(separator, "separator");
      return this;
   }

   public String getCommandName() {
      return this.commandName;
   }

   public CommandLine setCommandName(final String commandName) {
      this.commandName = CommandLine.Assert.notNull(commandName, "commandName");
      return this;
   }

   private static boolean empty(final String str) {
      return str == null || str.trim().length() == 0;
   }

   private static boolean empty(final Object[] array) {
      return array == null || array.length == 0;
   }

   private static boolean empty(final CommandLine.Help.Ansi.Text txt) {
      return txt == null || txt.plain.toString().trim().length() == 0;
   }

   private static String str(final String[] arr, final int i) {
      return arr != null && arr.length != 0 ? arr[i] : "";
   }

   private static boolean isBoolean(final Class<?> type) {
      return type == Boolean.class || type == boolean.class;
   }

   private static CommandLine toCommandLine(final Object obj) {
      return obj instanceof CommandLine ? (CommandLine)obj : new CommandLine(obj);
   }

   private static boolean isMultiValue(final Field field) {
      return isMultiValue(field.getType());
   }

   private static boolean isMultiValue(final Class<?> cls) {
      return cls.isArray() || Collection.class.isAssignableFrom(cls) || Map.class.isAssignableFrom(cls);
   }

   private static Class<?>[] getTypeAttribute(final Field field) {
      Class<?>[] explicit = field.isAnnotationPresent(CommandLine.Parameters.class)
         ? field.getAnnotation(CommandLine.Parameters.class).type()
         : field.getAnnotation(CommandLine.Option.class).type();
      if (explicit.length > 0) {
         return explicit;
      }

      if (field.getType().isArray()) {
         return new Class[]{field.getType().getComponentType()};
      }

      if (!isMultiValue(field)) {
         return new Class[]{field.getType()};
      }

      Type type = field.getGenericType();
      if (!(type instanceof ParameterizedType)) {
         return new Class[]{String.class, String.class};
      }

      ParameterizedType parameterizedType = (ParameterizedType)type;
      Type[] paramTypes = parameterizedType.getActualTypeArguments();
      Class<?>[] result = new Class[paramTypes.length];
      int i = 0;

      while (true) {
         if (i >= paramTypes.length) {
            return result;
         }

         if (paramTypes[i] instanceof Class) {
            result[i] = (Class<?>)paramTypes[i];
         } else {
            if (!(paramTypes[i] instanceof WildcardType)) {
               break;
            }

            WildcardType wildcardType = (WildcardType)paramTypes[i];
            Type[] lower = wildcardType.getLowerBounds();
            if (lower.length > 0 && lower[0] instanceof Class) {
               result[i] = (Class<?>)lower[0];
            } else {
               Type[] upper = wildcardType.getUpperBounds();
               if (upper.length <= 0 || !(upper[0] instanceof Class)) {
                  break;
               }

               result[i] = (Class<?>)upper[0];
            }
         }

         i++;
      }

      Arrays.fill(result, String.class);
      return result;
   }

   static void init(
      final Class<?> cls,
      final List<Field> requiredFields,
      final Map<String, Field> optionName2Field,
      final Map<Character, Field> singleCharOption2Field,
      final List<Field> positionalParametersFields
   ) {
      Field[] declaredFields = cls.getDeclaredFields();

      for (Field field : declaredFields) {
         field.setAccessible(true);
         if (field.isAnnotationPresent(CommandLine.Option.class)) {
            CommandLine.Option option = field.getAnnotation(CommandLine.Option.class);
            if (option.required()) {
               requiredFields.add(field);
            }

            for (String name : option.names()) {
               Field existing = optionName2Field.put(name, field);
               if (existing != null && existing != field) {
                  throw CommandLine.DuplicateOptionAnnotationsException.create(name, field, existing);
               }

               if (name.length() == 2 && name.startsWith("-")) {
                  char flag = name.charAt(1);
                  Field existing2 = singleCharOption2Field.put(flag, field);
                  if (existing2 != null && existing2 != field) {
                     throw CommandLine.DuplicateOptionAnnotationsException.create(name, field, existing2);
                  }
               }
            }
         }

         if (field.isAnnotationPresent(CommandLine.Parameters.class)) {
            if (field.isAnnotationPresent(CommandLine.Option.class)) {
               throw new CommandLine.DuplicateOptionAnnotationsException("A field can be either @Option or @Parameters, but '" + field.getName() + "' is both.");
            }

            positionalParametersFields.add(field);
            CommandLine.Range arity = CommandLine.Range.parameterArity(field);
            if (arity.min > 0) {
               requiredFields.add(field);
            }
         }
      }
   }

   static void validatePositionalParameters(final List<Field> positionalParametersFields) {
      int min = 0;

      for (Field field : positionalParametersFields) {
         CommandLine.Range index = CommandLine.Range.parameterIndex(field);
         if (index.min > min) {
            throw new CommandLine.ParameterIndexGapException(
               "Missing field annotated with @Parameter(index=" + min + "). Nearest field '" + field.getName() + "' has index=" + index.min
            );
         }

         min = Math.max(min, index.max);
         min = min == Integer.MAX_VALUE ? min : min + 1;
      }
   }

   private static <T> Stack<T> reverse(final Stack<T> stack) {
      Collections.reverse(stack);
      return stack;
   }

   private static final class Assert {
      static <T> T notNull(final T object, final String description) {
         if (object == null) {
            throw new NullPointerException(description);
         } else {
            return object;
         }
      }
   }

   private static final class BuiltIn {
      static class BigDecimalConverter implements CommandLine.ITypeConverter<BigDecimal> {
         public BigDecimal convert(final String value) {
            return new BigDecimal(value);
         }
      }

      static class BigIntegerConverter implements CommandLine.ITypeConverter<BigInteger> {
         public BigInteger convert(final String value) {
            return new BigInteger(value);
         }
      }

      static class BooleanConverter implements CommandLine.ITypeConverter<Boolean> {
         public Boolean convert(final String value) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
               throw new CommandLine.TypeConversionException("'" + value + "' is not a boolean");
            } else {
               return Boolean.parseBoolean(value);
            }
         }
      }

      static class ByteConverter implements CommandLine.ITypeConverter<Byte> {
         public Byte convert(final String value) {
            return Byte.valueOf(value);
         }
      }

      static class CharSequenceConverter implements CommandLine.ITypeConverter<CharSequence> {
         public String convert(final String value) {
            return value;
         }
      }

      static class CharacterConverter implements CommandLine.ITypeConverter<Character> {
         public Character convert(final String value) {
            if (value.length() > 1) {
               throw new CommandLine.TypeConversionException("'" + value + "' is not a single character");
            } else {
               return value.charAt(0);
            }
         }
      }

      static class CharsetConverter implements CommandLine.ITypeConverter<Charset> {
         public Charset convert(final String s) {
            return Charset.forName(s);
         }
      }

      static class DoubleConverter implements CommandLine.ITypeConverter<Double> {
         public Double convert(final String value) {
            return Double.valueOf(value);
         }
      }

      static class FileConverter implements CommandLine.ITypeConverter<File> {
         public File convert(final String value) {
            return new File(value);
         }
      }

      static class FloatConverter implements CommandLine.ITypeConverter<Float> {
         public Float convert(final String value) {
            return Float.valueOf(value);
         }
      }

      static class ISO8601DateConverter implements CommandLine.ITypeConverter<Date> {
         public Date convert(final String value) {
            try {
               return new SimpleDateFormat("yyyy-MM-dd").parse(value);
            } catch (ParseException e) {
               throw new CommandLine.TypeConversionException("'" + value + "' is not a yyyy-MM-dd date");
            }
         }
      }

      static class ISO8601TimeConverter implements CommandLine.ITypeConverter<Time> {
         public Time convert(final String value) {
            try {
               if (value.length() <= 5) {
                  return new Time(new SimpleDateFormat("HH:mm").parse(value).getTime());
               }

               if (value.length() <= 8) {
                  return new Time(new SimpleDateFormat("HH:mm:ss").parse(value).getTime());
               }

               if (value.length() <= 12) {
                  try {
                     return new Time(new SimpleDateFormat("HH:mm:ss.SSS").parse(value).getTime());
                  } catch (ParseException e2) {
                     return new Time(new SimpleDateFormat("HH:mm:ss,SSS").parse(value).getTime());
                  }
               }
            } catch (ParseException var4) {
            }

            throw new CommandLine.TypeConversionException("'" + value + "' is not a HH:mm[:ss[.SSS]] time");
         }
      }

      static class InetAddressConverter implements CommandLine.ITypeConverter<InetAddress> {
         public InetAddress convert(final String s) throws Exception {
            return InetAddress.getByName(s);
         }
      }

      static class IntegerConverter implements CommandLine.ITypeConverter<Integer> {
         public Integer convert(final String value) {
            return Integer.valueOf(value);
         }
      }

      static class LongConverter implements CommandLine.ITypeConverter<Long> {
         public Long convert(final String value) {
            return Long.valueOf(value);
         }
      }

      static class PathConverter implements CommandLine.ITypeConverter<Path> {
         public Path convert(final String value) {
            return Paths.get(value);
         }
      }

      static class PatternConverter implements CommandLine.ITypeConverter<Pattern> {
         public Pattern convert(final String s) {
            return Pattern.compile(s);
         }
      }

      static class ShortConverter implements CommandLine.ITypeConverter<Short> {
         public Short convert(final String value) {
            return Short.valueOf(value);
         }
      }

      static class StringBuilderConverter implements CommandLine.ITypeConverter<StringBuilder> {
         public StringBuilder convert(final String value) {
            return new StringBuilder(value);
         }
      }

      static class StringConverter implements CommandLine.ITypeConverter<String> {
         public String convert(final String value) {
            return value;
         }
      }

      static class URIConverter implements CommandLine.ITypeConverter<URI> {
         public URI convert(final String value) throws URISyntaxException {
            return new URI(value);
         }
      }

      static class URLConverter implements CommandLine.ITypeConverter<URL> {
         public URL convert(final String value) throws MalformedURLException {
            return new URL(value);
         }
      }

      static class UUIDConverter implements CommandLine.ITypeConverter<UUID> {
         public UUID convert(final String s) throws Exception {
            return UUID.fromString(s);
         }
      }
   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target({ElementType.TYPE, ElementType.LOCAL_VARIABLE, ElementType.PACKAGE})
   public @interface Command {
      String name() default "<main class>";

      Class<?>[] subcommands() default {};

      String separator() default "=";

      String[] version() default {};

      String headerHeading() default "";

      String[] header() default {};

      String synopsisHeading() default "Usage: ";

      boolean abbreviateSynopsis() default false;

      String[] customSynopsis() default {};

      String descriptionHeading() default "";

      String[] description() default {};

      String parameterListHeading() default "";

      String optionListHeading() default "";

      boolean sortOptions() default true;

      char requiredOptionMarker() default ' ';

      boolean showDefaultValues() default false;

      String commandListHeading() default "Commands:%n";

      String footerHeading() default "";

      String[] footer() default {};
   }

   public static class DefaultExceptionHandler implements CommandLine.IExceptionHandler {
      @Override
      public List<Object> handleException(
         final CommandLine.ParameterException ex, final PrintStream out, final CommandLine.Help.Ansi ansi, final String... args
      ) {
         out.println(ex.getMessage());
         ex.getCommandLine().usage(out, ansi);
         return Collections.emptyList();
      }
   }

   public static class DuplicateOptionAnnotationsException extends CommandLine.InitializationException {
      private static final long serialVersionUID = -3355128012575075641L;

      public DuplicateOptionAnnotationsException(final String msg) {
         super(msg);
      }

      private static CommandLine.DuplicateOptionAnnotationsException create(final String name, final Field field1, final Field field2) {
         return new CommandLine.DuplicateOptionAnnotationsException(
            "Option name '"
               + name
               + "' is used by both "
               + field1.getDeclaringClass().getName()
               + "."
               + field1.getName()
               + " and "
               + field2.getDeclaringClass().getName()
               + "."
               + field2.getName()
         );
      }
   }

   public static class ExecutionException extends CommandLine.PicocliException {
      private static final long serialVersionUID = 7764539594267007998L;
      private final CommandLine commandLine;

      public ExecutionException(final CommandLine commandLine, final String msg) {
         super(msg);
         this.commandLine = CommandLine.Assert.notNull(commandLine, "commandLine");
      }

      public ExecutionException(final CommandLine commandLine, final String msg, final Exception ex) {
         super(msg, ex);
         this.commandLine = CommandLine.Assert.notNull(commandLine, "commandLine");
      }

      public CommandLine getCommandLine() {
         return this.commandLine;
      }
   }

   public static class Help {
      protected static final String DEFAULT_COMMAND_NAME = "<main class>";
      protected static final String DEFAULT_SEPARATOR = "=";
      private static final int usageHelpWidth = 80;
      private static final int optionsColumnWidth = 29;
      private final Object command;
      private final Map<String, CommandLine.Help> commands = new LinkedHashMap<>();
      final CommandLine.Help.ColorScheme colorScheme;
      public final List<Field> optionFields;
      public final List<Field> positionalParametersFields;
      public String separator;
      public String commandName = "<main class>";
      public String[] description = new String[0];
      public String[] customSynopsis = new String[0];
      public String[] header = new String[0];
      public String[] footer = new String[0];
      public CommandLine.Help.IParamLabelRenderer parameterLabelRenderer;
      public Boolean abbreviateSynopsis;
      public Boolean sortOptions;
      public Boolean showDefaultValues;
      public Character requiredOptionMarker;
      public String headerHeading;
      public String synopsisHeading;
      public String descriptionHeading;
      public String parameterListHeading;
      public String optionListHeading;
      public String commandListHeading;
      public String footerHeading;

      public Help(final Object command) {
         this(command, CommandLine.Help.Ansi.AUTO);
      }

      public Help(final Object command, final CommandLine.Help.Ansi ansi) {
         this(command, defaultColorScheme(ansi));
      }

      public Help(final Object command, final CommandLine.Help.ColorScheme colorScheme) {
         this.command = CommandLine.Assert.notNull(command, "command");
         this.colorScheme = CommandLine.Assert.notNull(colorScheme, "colorScheme").applySystemProperties();
         List<Field> options = new ArrayList<>();
         List<Field> operands = new ArrayList<>();

         for (Class<?> cls = command.getClass(); cls != null; cls = cls.getSuperclass()) {
            for (Field field : cls.getDeclaredFields()) {
               field.setAccessible(true);
               if (field.isAnnotationPresent(CommandLine.Option.class)) {
                  CommandLine.Option option = field.getAnnotation(CommandLine.Option.class);
                  if (!option.hidden()) {
                     options.add(field);
                  }
               }

               if (field.isAnnotationPresent(CommandLine.Parameters.class)) {
                  operands.add(field);
               }
            }

            if (cls.isAnnotationPresent(CommandLine.Command.class)) {
               CommandLine.Command cmd = cls.getAnnotation(CommandLine.Command.class);
               if ("<main class>".equals(this.commandName)) {
                  this.commandName = cmd.name();
               }

               this.separator = this.separator == null ? cmd.separator() : this.separator;
               this.abbreviateSynopsis = this.abbreviateSynopsis == null ? cmd.abbreviateSynopsis() : this.abbreviateSynopsis;
               this.sortOptions = this.sortOptions == null ? cmd.sortOptions() : this.sortOptions;
               this.requiredOptionMarker = this.requiredOptionMarker == null ? cmd.requiredOptionMarker() : this.requiredOptionMarker;
               this.showDefaultValues = this.showDefaultValues == null ? cmd.showDefaultValues() : this.showDefaultValues;
               this.customSynopsis = CommandLine.empty(this.customSynopsis) ? cmd.customSynopsis() : this.customSynopsis;
               this.description = CommandLine.empty(this.description) ? cmd.description() : this.description;
               this.header = CommandLine.empty(this.header) ? cmd.header() : this.header;
               this.footer = CommandLine.empty(this.footer) ? cmd.footer() : this.footer;
               this.headerHeading = CommandLine.empty(this.headerHeading) ? cmd.headerHeading() : this.headerHeading;
               this.synopsisHeading = !CommandLine.empty(this.synopsisHeading) && !"Usage: ".equals(this.synopsisHeading)
                  ? this.synopsisHeading
                  : cmd.synopsisHeading();
               this.descriptionHeading = CommandLine.empty(this.descriptionHeading) ? cmd.descriptionHeading() : this.descriptionHeading;
               this.parameterListHeading = CommandLine.empty(this.parameterListHeading) ? cmd.parameterListHeading() : this.parameterListHeading;
               this.optionListHeading = CommandLine.empty(this.optionListHeading) ? cmd.optionListHeading() : this.optionListHeading;
               this.commandListHeading = !CommandLine.empty(this.commandListHeading) && !"Commands:%n".equals(this.commandListHeading)
                  ? this.commandListHeading
                  : cmd.commandListHeading();
               this.footerHeading = CommandLine.empty(this.footerHeading) ? cmd.footerHeading() : this.footerHeading;
            }
         }

         this.sortOptions = this.sortOptions == null || this.sortOptions;
         this.abbreviateSynopsis = this.abbreviateSynopsis != null && this.abbreviateSynopsis;
         this.requiredOptionMarker = this.requiredOptionMarker == null ? ' ' : this.requiredOptionMarker;
         this.showDefaultValues = this.showDefaultValues != null && this.showDefaultValues;
         this.synopsisHeading = this.synopsisHeading == null ? "Usage: " : this.synopsisHeading;
         this.commandListHeading = this.commandListHeading == null ? "Commands:%n" : this.commandListHeading;
         this.separator = this.separator == null ? "=" : this.separator;
         this.parameterLabelRenderer = this.createDefaultParamLabelRenderer();
         Collections.sort(operands, new CommandLine.PositionalParametersSorter());
         this.positionalParametersFields = Collections.unmodifiableList(operands);
         this.optionFields = Collections.unmodifiableList(options);
      }

      public CommandLine.Help addAllSubcommands(final Map<String, CommandLine> commands) {
         if (commands != null) {
            for (Entry<String, CommandLine> entry : commands.entrySet()) {
               this.addSubcommand(entry.getKey(), entry.getValue().getCommand());
            }
         }

         return this;
      }

      public CommandLine.Help addSubcommand(final String commandName, final Object command) {
         this.commands.put(commandName, new CommandLine.Help(command));
         return this;
      }

      @Deprecated
      public String synopsis() {
         return this.synopsis(0);
      }

      public String synopsis(final int synopsisHeadingLength) {
         if (!CommandLine.empty(this.customSynopsis)) {
            return this.customSynopsis();
         } else {
            return this.abbreviateSynopsis
               ? this.abbreviatedSynopsis()
               : this.detailedSynopsis(synopsisHeadingLength, createShortOptionArityAndNameComparator(), true);
         }
      }

      public String abbreviatedSynopsis() {
         StringBuilder sb = new StringBuilder();
         if (!this.optionFields.isEmpty()) {
            sb.append(" [OPTIONS]");
         }

         for (Field positionalParam : this.positionalParametersFields) {
            if (!positionalParam.getAnnotation(CommandLine.Parameters.class).hidden()) {
               sb.append(' ').append(this.parameterLabelRenderer.renderParameterLabel(positionalParam, this.ansi(), this.colorScheme.parameterStyles));
            }
         }

         return this.colorScheme.commandText(this.commandName).toString() + sb.toString() + System.getProperty("line.separator");
      }

      @Deprecated
      public String detailedSynopsis(final Comparator<Field> optionSort, final boolean clusterBooleanOptions) {
         return this.detailedSynopsis(0, optionSort, clusterBooleanOptions);
      }

      public String detailedSynopsis(final int synopsisHeadingLength, final Comparator<Field> optionSort, final boolean clusterBooleanOptions) {
         CommandLine.Help.Ansi.Text optionText = this.ansi().new Text(0);
         List<Field> fields = new ArrayList<>(this.optionFields);
         if (optionSort != null) {
            Collections.sort(fields, optionSort);
         }

         if (clusterBooleanOptions) {
            List<Field> booleanOptions = new ArrayList<>();
            StringBuilder clusteredRequired = new StringBuilder("-");
            StringBuilder clusteredOptional = new StringBuilder("-");

            for (Field field : fields) {
               if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                  CommandLine.Option option = field.getAnnotation(CommandLine.Option.class);
                  String shortestName = CommandLine.Help.ShortestFirst.sort(option.names())[0];
                  if (shortestName.length() == 2 && shortestName.startsWith("-")) {
                     booleanOptions.add(field);
                     if (option.required()) {
                        clusteredRequired.append(shortestName.substring(1));
                     } else {
                        clusteredOptional.append(shortestName.substring(1));
                     }
                  }
               }
            }

            fields.removeAll(booleanOptions);
            if (clusteredRequired.length() > 1) {
               optionText = optionText.append(" ").append(this.colorScheme.optionText(clusteredRequired.toString()));
            }

            if (clusteredOptional.length() > 1) {
               optionText = optionText.append(" [").append(this.colorScheme.optionText(clusteredOptional.toString())).append("]");
            }
         }

         for (Field field : fields) {
            CommandLine.Option option = field.getAnnotation(CommandLine.Option.class);
            if (!option.hidden()) {
               if (option.required()) {
                  optionText = this.appendOptionSynopsis(optionText, field, CommandLine.Help.ShortestFirst.sort(option.names())[0], " ", "");
                  if (CommandLine.isMultiValue(field)) {
                     optionText = this.appendOptionSynopsis(optionText, field, CommandLine.Help.ShortestFirst.sort(option.names())[0], " [", "]...");
                  }
               } else {
                  optionText = this.appendOptionSynopsis(optionText, field, CommandLine.Help.ShortestFirst.sort(option.names())[0], " [", "]");
                  if (CommandLine.isMultiValue(field)) {
                     optionText = optionText.append("...");
                  }
               }
            }
         }

         for (Field positionalParam : this.positionalParametersFields) {
            if (!positionalParam.getAnnotation(CommandLine.Parameters.class).hidden()) {
               optionText = optionText.append(" ");
               CommandLine.Help.Ansi.Text label = this.parameterLabelRenderer
                  .renderParameterLabel(positionalParam, this.colorScheme.ansi(), this.colorScheme.parameterStyles);
               optionText = optionText.append(label);
            }
         }

         int firstColumnLength = this.commandName.length() + synopsisHeadingLength;
         CommandLine.Help.TextTable textTable = new CommandLine.Help.TextTable(this.ansi(), firstColumnLength, 80 - firstColumnLength);
         textTable.indentWrappedLines = 1;
         CommandLine.Help.Ansi.Text PADDING = CommandLine.Help.Ansi.OFF.new Text(stringOf('X', synopsisHeadingLength));
         textTable.addRowValues(PADDING.append(this.colorScheme.commandText(this.commandName)), optionText);
         return textTable.toString().substring(synopsisHeadingLength);
      }

      private CommandLine.Help.Ansi.Text appendOptionSynopsis(
         final CommandLine.Help.Ansi.Text optionText, final Field field, final String optionName, final String prefix, final String suffix
      ) {
         CommandLine.Help.Ansi.Text optionParamText = this.parameterLabelRenderer
            .renderParameterLabel(field, this.colorScheme.ansi(), this.colorScheme.optionParamStyles);
         return optionText.append(prefix).append(this.colorScheme.optionText(optionName)).append(optionParamText).append(suffix);
      }

      public int synopsisHeadingLength() {
         String[] lines = CommandLine.Help.Ansi.OFF.new Text(this.synopsisHeading).toString().split("\\r?\\n|\\r|%n", -1);
         return lines[lines.length - 1].length();
      }

      public String optionList() {
         Comparator<Field> sortOrder = this.sortOptions != null && !this.sortOptions ? null : createShortOptionNameComparator();
         return this.optionList(this.createDefaultLayout(), sortOrder, this.parameterLabelRenderer);
      }

      public String optionList(
         final CommandLine.Help.Layout layout, final Comparator<Field> optionSort, final CommandLine.Help.IParamLabelRenderer valueLabelRenderer
      ) {
         List<Field> fields = new ArrayList<>(this.optionFields);
         if (optionSort != null) {
            Collections.sort(fields, optionSort);
         }

         layout.addOptions(fields, valueLabelRenderer);
         return layout.toString();
      }

      public String parameterList() {
         return this.parameterList(this.createDefaultLayout(), this.parameterLabelRenderer);
      }

      public String parameterList(final CommandLine.Help.Layout layout, final CommandLine.Help.IParamLabelRenderer paramLabelRenderer) {
         layout.addPositionalParameters(this.positionalParametersFields, paramLabelRenderer);
         return layout.toString();
      }

      private static String heading(final CommandLine.Help.Ansi ansi, final String values, final Object... params) {
         StringBuilder sb = join(ansi, new String[]{values}, new StringBuilder(), params);
         String result = sb.toString();
         result = result.endsWith(System.getProperty("line.separator"))
            ? result.substring(0, result.length() - System.getProperty("line.separator").length())
            : result;
         return result + new String(spaces(countTrailingSpaces(values)));
      }

      private static char[] spaces(final int length) {
         char[] result = new char[length];
         Arrays.fill(result, ' ');
         return result;
      }

      private static int countTrailingSpaces(final String str) {
         if (str == null) {
            return 0;
         }

         int trailingSpaces = 0;

         for (int i = str.length() - 1; i >= 0 && str.charAt(i) == ' '; i--) {
            trailingSpaces++;
         }

         return trailingSpaces;
      }

      public static StringBuilder join(final CommandLine.Help.Ansi ansi, final String[] values, final StringBuilder sb, final Object... params) {
         if (values != null) {
            CommandLine.Help.TextTable table = new CommandLine.Help.TextTable(ansi, 80);
            table.indentWrappedLines = 0;

            for (String summaryLine : values) {
               CommandLine.Help.Ansi.Text[] lines = ansi.new Text(format(summaryLine, params)).splitLines();

               for (CommandLine.Help.Ansi.Text line : lines) {
                  table.addRowValues(line);
               }
            }

            table.toString(sb);
         }

         return sb;
      }

      private static String format(final String formatString, final Object... params) {
         return formatString == null ? "" : String.format(formatString, params);
      }

      public String customSynopsis(final Object... params) {
         return join(this.ansi(), this.customSynopsis, new StringBuilder(), params).toString();
      }

      public String description(final Object... params) {
         return join(this.ansi(), this.description, new StringBuilder(), params).toString();
      }

      public String header(final Object... params) {
         return join(this.ansi(), this.header, new StringBuilder(), params).toString();
      }

      public String footer(final Object... params) {
         return join(this.ansi(), this.footer, new StringBuilder(), params).toString();
      }

      public String headerHeading(final Object... params) {
         return heading(this.ansi(), this.headerHeading, params);
      }

      public String synopsisHeading(final Object... params) {
         return heading(this.ansi(), this.synopsisHeading, params);
      }

      public String descriptionHeading(final Object... params) {
         return CommandLine.empty(this.descriptionHeading) ? "" : heading(this.ansi(), this.descriptionHeading, params);
      }

      public String parameterListHeading(final Object... params) {
         return this.positionalParametersFields.isEmpty() ? "" : heading(this.ansi(), this.parameterListHeading, params);
      }

      public String optionListHeading(final Object... params) {
         return this.optionFields.isEmpty() ? "" : heading(this.ansi(), this.optionListHeading, params);
      }

      public String commandListHeading(final Object... params) {
         return this.commands.isEmpty() ? "" : heading(this.ansi(), this.commandListHeading, params);
      }

      public String footerHeading(final Object... params) {
         return heading(this.ansi(), this.footerHeading, params);
      }

      public String commandList() {
         if (this.commands.isEmpty()) {
            return "";
         }

         int commandLength = maxLength(this.commands.keySet());
         CommandLine.Help.TextTable textTable = new CommandLine.Help.TextTable(
            this.ansi(),
            new CommandLine.Help.Column(commandLength + 2, 2, CommandLine.Help.Column.Overflow.SPAN),
            new CommandLine.Help.Column(80 - (commandLength + 2), 2, CommandLine.Help.Column.Overflow.WRAP)
         );

         for (Entry<String, CommandLine.Help> entry : this.commands.entrySet()) {
            CommandLine.Help command = entry.getValue();
            String header = command.header != null && command.header.length > 0
               ? command.header[0]
               : (command.description != null && command.description.length > 0 ? command.description[0] : "");
            textTable.addRowValues(this.colorScheme.commandText(entry.getKey()), this.ansi().new Text(header));
         }

         return textTable.toString();
      }

      private static int maxLength(final Collection<String> any) {
         List<String> strings = new ArrayList<>(any);
         Collections.sort(strings, Collections.reverseOrder(shortestFirst()));
         return strings.get(0).length();
      }

      private static String join(final String[] names, final int offset, final int length, final String separator) {
         if (names == null) {
            return "";
         }

         StringBuilder result = new StringBuilder();

         for (int i = offset; i < offset + length; i++) {
            result.append(i > offset ? separator : "").append(names[i]);
         }

         return result.toString();
      }

      private static String stringOf(final char chr, final int length) {
         char[] buff = new char[length];
         Arrays.fill(buff, chr);
         return new String(buff);
      }

      public CommandLine.Help.Layout createDefaultLayout() {
         return new CommandLine.Help.Layout(
            this.colorScheme,
            new CommandLine.Help.TextTable(this.colorScheme.ansi()),
            this.createDefaultOptionRenderer(),
            this.createDefaultParameterRenderer()
         );
      }

      public CommandLine.Help.IOptionRenderer createDefaultOptionRenderer() {
         CommandLine.Help.DefaultOptionRenderer result = new CommandLine.Help.DefaultOptionRenderer();
         result.requiredMarker = String.valueOf(this.requiredOptionMarker);
         if (this.showDefaultValues != null && this.showDefaultValues) {
            result.command = this.command;
         }

         return result;
      }

      public static CommandLine.Help.IOptionRenderer createMinimalOptionRenderer() {
         return new CommandLine.Help.MinimalOptionRenderer();
      }

      public CommandLine.Help.IParameterRenderer createDefaultParameterRenderer() {
         CommandLine.Help.DefaultParameterRenderer result = new CommandLine.Help.DefaultParameterRenderer();
         result.requiredMarker = String.valueOf(this.requiredOptionMarker);
         return result;
      }

      public static CommandLine.Help.IParameterRenderer createMinimalParameterRenderer() {
         return new CommandLine.Help.MinimalParameterRenderer();
      }

      public static CommandLine.Help.IParamLabelRenderer createMinimalParamLabelRenderer() {
         return new CommandLine.Help.IParamLabelRenderer() {
            @Override
            public CommandLine.Help.Ansi.Text renderParameterLabel(
               final Field field, final CommandLine.Help.Ansi ansi, final List<CommandLine.Help.Ansi.IStyle> styles
            ) {
               String text = CommandLine.Help.DefaultParamLabelRenderer.renderParameterName(field);
               return ansi.apply(text, styles);
            }

            @Override
            public String separator() {
               return "";
            }
         };
      }

      public CommandLine.Help.IParamLabelRenderer createDefaultParamLabelRenderer() {
         return new CommandLine.Help.DefaultParamLabelRenderer(this.separator);
      }

      public static Comparator<Field> createShortOptionNameComparator() {
         return new CommandLine.Help.SortByShortestOptionNameAlphabetically();
      }

      public static Comparator<Field> createShortOptionArityAndNameComparator() {
         return new CommandLine.Help.SortByOptionArityAndNameAlphabetically();
      }

      public static Comparator<String> shortestFirst() {
         return new CommandLine.Help.ShortestFirst();
      }

      public CommandLine.Help.Ansi ansi() {
         return this.colorScheme.ansi;
      }

      public static CommandLine.Help.ColorScheme defaultColorScheme(final CommandLine.Help.Ansi ansi) {
         return new CommandLine.Help.ColorScheme(ansi)
            .commands(CommandLine.Help.Ansi.Style.bold)
            .options(CommandLine.Help.Ansi.Style.fg_yellow)
            .parameters(CommandLine.Help.Ansi.Style.fg_yellow)
            .optionParams(CommandLine.Help.Ansi.Style.italic);
      }

      public enum Ansi {
         AUTO,
         ON,
         OFF;

         static final CommandLine.Help.Ansi.Text EMPTY_TEXT = OFF.new Text(0);
         static final boolean isWindows = System.getProperty("os.name").startsWith("Windows");
         static final boolean isXterm = System.getenv("TERM") != null && System.getenv("TERM").startsWith("xterm");
         static final boolean ISATTY = calcTTY();

         static final boolean calcTTY() {
            if (isWindows && isXterm) {
               return true;
            }

            try {
               return System.class.getDeclaredMethod("console").invoke(null) != null;
            } catch (Throwable reflectionFailed) {
               return true;
            }
         }

         private static boolean ansiPossible() {
            return ISATTY && (!isWindows || isXterm);
         }

         public boolean enabled() {
            if (this == ON) {
               return true;
            } else if (this == OFF) {
               return false;
            } else {
               return System.getProperty("picocli.ansi") == null ? ansiPossible() : Boolean.getBoolean("picocli.ansi");
            }
         }

         public CommandLine.Help.Ansi.Text apply(final String plainText, final List<CommandLine.Help.Ansi.IStyle> styles) {
            if (plainText.length() == 0) {
               return new CommandLine.Help.Ansi.Text(0);
            }

            CommandLine.Help.Ansi.Text result = new CommandLine.Help.Ansi.Text(plainText.length());
            CommandLine.Help.Ansi.IStyle[] all = styles.toArray(new CommandLine.Help.Ansi.IStyle[styles.size()]);
            result.sections
               .add(
                  new CommandLine.Help.Ansi.StyledSection(
                     0,
                     plainText.length(),
                     CommandLine.Help.Ansi.Style.on(all),
                     CommandLine.Help.Ansi.Style.off(reverse(all)) + CommandLine.Help.Ansi.Style.reset.off()
                  )
               );
            result.plain.append(plainText);
            result.length = result.plain.length();
            return result;
         }

         private static <T> T[] reverse(final T[] all) {
            for (int i = 0; i < all.length / 2; i++) {
               T temp = all[i];
               all[i] = all[all.length - i - 1];
               all[all.length - i - 1] = temp;
            }

            return all;
         }

         public interface IStyle {
            String CSI = "\u001b[";

            String on();

            String off();
         }

         static class Palette256Color implements CommandLine.Help.Ansi.IStyle {
            private final int fgbg;
            private final int color;

            Palette256Color(final boolean foreground, final String color) {
               this.fgbg = foreground ? 38 : 48;
               String[] rgb = color.split(";");
               if (rgb.length == 3) {
                  this.color = 16 + 36 * Integer.decode(rgb[0]) + 6 * Integer.decode(rgb[1]) + Integer.decode(rgb[2]);
               } else {
                  this.color = Integer.decode(color);
               }
            }

            @Override
            public String on() {
               return String.format("\u001b[%d;5;%dm", this.fgbg, this.color);
            }

            @Override
            public String off() {
               return "\u001b[" + (this.fgbg + 1) + "m";
            }
         }

         public enum Style implements CommandLine.Help.Ansi.IStyle {
            reset(0, 0),
            bold(1, 21),
            faint(2, 22),
            italic(3, 23),
            underline(4, 24),
            blink(5, 25),
            reverse(7, 27),
            fg_black(30, 39),
            fg_red(31, 39),
            fg_green(32, 39),
            fg_yellow(33, 39),
            fg_blue(34, 39),
            fg_magenta(35, 39),
            fg_cyan(36, 39),
            fg_white(37, 39),
            bg_black(40, 49),
            bg_red(41, 49),
            bg_green(42, 49),
            bg_yellow(43, 49),
            bg_blue(44, 49),
            bg_magenta(45, 49),
            bg_cyan(46, 49),
            bg_white(47, 49);

            private final int startCode;
            private final int endCode;

            Style(final int startCode, final int endCode) {
               this.startCode = startCode;
               this.endCode = endCode;
            }

            @Override
            public String on() {
               return "\u001b[" + this.startCode + "m";
            }

            @Override
            public String off() {
               return "\u001b[" + this.endCode + "m";
            }

            public static String on(final CommandLine.Help.Ansi.IStyle... styles) {
               StringBuilder result = new StringBuilder();

               for (CommandLine.Help.Ansi.IStyle style : styles) {
                  result.append(style.on());
               }

               return result.toString();
            }

            public static String off(final CommandLine.Help.Ansi.IStyle... styles) {
               StringBuilder result = new StringBuilder();

               for (CommandLine.Help.Ansi.IStyle style : styles) {
                  result.append(style.off());
               }

               return result.toString();
            }

            public static CommandLine.Help.Ansi.IStyle fg(final String str) {
               try {
                  return valueOf(str.toLowerCase(Locale.ENGLISH));
               } catch (Exception var3) {
                  try {
                     return valueOf("fg_" + str.toLowerCase(Locale.ENGLISH));
                  } catch (Exception var2) {
                     return new CommandLine.Help.Ansi.Palette256Color(true, str);
                  }
               }
            }

            public static CommandLine.Help.Ansi.IStyle bg(final String str) {
               try {
                  return valueOf(str.toLowerCase(Locale.ENGLISH));
               } catch (Exception var3) {
                  try {
                     return valueOf("bg_" + str.toLowerCase(Locale.ENGLISH));
                  } catch (Exception var2) {
                     return new CommandLine.Help.Ansi.Palette256Color(false, str);
                  }
               }
            }

            public static CommandLine.Help.Ansi.IStyle[] parse(final String commaSeparatedCodes) {
               String[] codes = commaSeparatedCodes.split(",");
               CommandLine.Help.Ansi.IStyle[] styles = new CommandLine.Help.Ansi.IStyle[codes.length];

               for (int i = 0; i < codes.length; i++) {
                  if (codes[i].toLowerCase(Locale.ENGLISH).startsWith("fg(")) {
                     int end = codes[i].indexOf(41);
                     styles[i] = fg(codes[i].substring(3, end < 0 ? codes[i].length() : end));
                  } else if (codes[i].toLowerCase(Locale.ENGLISH).startsWith("bg(")) {
                     int end = codes[i].indexOf(41);
                     styles[i] = bg(codes[i].substring(3, end < 0 ? codes[i].length() : end));
                  } else {
                     styles[i] = fg(codes[i]);
                  }
               }

               return styles;
            }
         }

         private static class StyledSection {
            final int startIndex;
            final int length;
            final String startStyles;
            final String endStyles;

            StyledSection(final int start, final int len, final String style1, final String style2) {
               this.startIndex = start;
               this.length = len;
               this.startStyles = style1;
               this.endStyles = style2;
            }

            CommandLine.Help.Ansi.StyledSection withStartIndex(final int newStart) {
               return new CommandLine.Help.Ansi.StyledSection(newStart, this.length, this.startStyles, this.endStyles);
            }
         }

         public class Text implements Cloneable {
            private final int maxLength;
            private int from;
            private int length;
            private StringBuilder plain = new StringBuilder();
            private List<CommandLine.Help.Ansi.StyledSection> sections = new ArrayList<>();

            public Text(final int maxLength) {
               this.maxLength = maxLength;
            }

            public Text(final String input) {
               this.maxLength = -1;
               this.plain.setLength(0);
               int i = 0;

               while (true) {
                  int j = input.indexOf("@|", i);
                  if (j == -1) {
                     if (i == 0) {
                        this.plain.append(input);
                        this.length = this.plain.length();
                        return;
                     }

                     this.plain.append(input.substring(i, input.length()));
                     this.length = this.plain.length();
                     return;
                  }

                  this.plain.append(input.substring(i, j));
                  int k = input.indexOf("|@", j);
                  if (k == -1) {
                     this.plain.append(input);
                     this.length = this.plain.length();
                     return;
                  }

                  j += 2;
                  String spec = input.substring(j, k);
                  String[] items = spec.split(" ", 2);
                  if (items.length == 1) {
                     this.plain.append(input);
                     this.length = this.plain.length();
                     return;
                  }

                  CommandLine.Help.Ansi.IStyle[] styles = CommandLine.Help.Ansi.Style.parse(items[0]);
                  this.addStyledSection(
                     this.plain.length(),
                     items[1].length(),
                     CommandLine.Help.Ansi.Style.on(styles),
                     CommandLine.Help.Ansi.Style.off(CommandLine.Help.Ansi.reverse(styles)) + CommandLine.Help.Ansi.Style.reset.off()
                  );
                  this.plain.append(items[1]);
                  i = k + 2;
               }
            }

            private void addStyledSection(final int start, final int length, final String startStyle, final String endStyle) {
               this.sections.add(new CommandLine.Help.Ansi.StyledSection(start, length, startStyle, endStyle));
            }

            @Override
            public Object clone() {
               try {
                  return super.clone();
               } catch (CloneNotSupportedException e) {
                  throw new IllegalStateException(e);
               }
            }

            public CommandLine.Help.Ansi.Text[] splitLines() {
               List<CommandLine.Help.Ansi.Text> result = new ArrayList<>();
               boolean trailingEmptyString = false;
               int start = 0;
               int end = 0;

               for (int i = 0; i < this.plain.length(); end = ++i) {
                  char c;
                  boolean eol;
                  boolean var10001;
                  label48: {
                     c = this.plain.charAt(i);
                     eol = c == '\n';
                     if (c == '\r' && i + 1 < this.plain.length() && this.plain.charAt(i + 1) == '\n') {
                        if (++i > 0) {
                           var10001 = true;
                           break label48;
                        }
                     }

                     var10001 = false;
                  }

                  eol |= var10001;
                  eol |= c == '\r';
                  if (eol) {
                     result.add(this.substring(start, end));
                     trailingEmptyString = i == this.plain.length() - 1;
                     start = i + 1;
                  }
               }

               if (start < this.plain.length() || trailingEmptyString) {
                  result.add(this.substring(start, this.plain.length()));
               }

               return result.toArray(new CommandLine.Help.Ansi.Text[result.size()]);
            }

            public CommandLine.Help.Ansi.Text substring(final int start) {
               return this.substring(start, this.length);
            }

            public CommandLine.Help.Ansi.Text substring(final int start, final int end) {
               CommandLine.Help.Ansi.Text result = (CommandLine.Help.Ansi.Text)this.clone();
               result.from = this.from + start;
               result.length = end - start;
               return result;
            }

            public CommandLine.Help.Ansi.Text append(final String string) {
               return this.append(Ansi.this.new Text(string));
            }

            public CommandLine.Help.Ansi.Text append(final CommandLine.Help.Ansi.Text other) {
               CommandLine.Help.Ansi.Text result = (CommandLine.Help.Ansi.Text)this.clone();
               result.plain = new StringBuilder(this.plain.toString().substring(this.from, this.from + this.length));
               result.from = 0;
               result.sections = new ArrayList<>();

               for (CommandLine.Help.Ansi.StyledSection section : this.sections) {
                  result.sections.add(section.withStartIndex(section.startIndex - this.from));
               }

               result.plain.append(other.plain.toString().substring(other.from, other.from + other.length));

               for (CommandLine.Help.Ansi.StyledSection section : other.sections) {
                  int index = result.length + section.startIndex - other.from;
                  result.sections.add(section.withStartIndex(index));
               }

               result.length = result.plain.length();
               return result;
            }

            public void getStyledChars(final int from, final int length, final CommandLine.Help.Ansi.Text destination, final int offset) {
               if (destination.length < offset) {
                  for (int i = destination.length; i < offset; i++) {
                     destination.plain.append(' ');
                  }

                  destination.length = offset;
               }

               for (CommandLine.Help.Ansi.StyledSection section : this.sections) {
                  destination.sections.add(section.withStartIndex(section.startIndex - from + destination.length));
               }

               destination.plain.append(this.plain.toString().substring(from, from + length));
               destination.length = destination.plain.length();
            }

            public String plainString() {
               return this.plain.toString().substring(this.from, this.from + this.length);
            }

            @Override
            public boolean equals(final Object obj) {
               return this.toString().equals(String.valueOf(obj));
            }

            @Override
            public int hashCode() {
               return this.toString().hashCode();
            }

            @Override
            public String toString() {
               if (!Ansi.this.enabled()) {
                  return this.plain.toString().substring(this.from, this.from + this.length);
               }

               if (this.length == 0) {
                  return "";
               }

               StringBuilder sb = new StringBuilder(this.plain.length() + 20 * this.sections.size());
               CommandLine.Help.Ansi.StyledSection current = null;
               int end = Math.min(this.from + this.length, this.plain.length());

               for (int i = this.from; i < end; i++) {
                  CommandLine.Help.Ansi.StyledSection section = this.findSectionContaining(i);
                  if (section != current) {
                     if (current != null) {
                        sb.append(current.endStyles);
                     }

                     if (section != null) {
                        sb.append(section.startStyles);
                     }

                     current = section;
                  }

                  sb.append(this.plain.charAt(i));
               }

               if (current != null) {
                  sb.append(current.endStyles);
               }

               return sb.toString();
            }

            private CommandLine.Help.Ansi.StyledSection findSectionContaining(final int index) {
               for (CommandLine.Help.Ansi.StyledSection section : this.sections) {
                  if (index >= section.startIndex && index < section.startIndex + section.length) {
                     return section;
                  }
               }

               return null;
            }
         }
      }

      public static class ColorScheme {
         public final List<CommandLine.Help.Ansi.IStyle> commandStyles = new ArrayList<>();
         public final List<CommandLine.Help.Ansi.IStyle> optionStyles = new ArrayList<>();
         public final List<CommandLine.Help.Ansi.IStyle> parameterStyles = new ArrayList<>();
         public final List<CommandLine.Help.Ansi.IStyle> optionParamStyles = new ArrayList<>();
         private final CommandLine.Help.Ansi ansi;

         public ColorScheme() {
            this(CommandLine.Help.Ansi.AUTO);
         }

         public ColorScheme(final CommandLine.Help.Ansi ansi) {
            this.ansi = CommandLine.Assert.notNull(ansi, "ansi");
         }

         public CommandLine.Help.ColorScheme commands(final CommandLine.Help.Ansi.IStyle... styles) {
            return this.addAll(this.commandStyles, styles);
         }

         public CommandLine.Help.ColorScheme options(final CommandLine.Help.Ansi.IStyle... styles) {
            return this.addAll(this.optionStyles, styles);
         }

         public CommandLine.Help.ColorScheme parameters(final CommandLine.Help.Ansi.IStyle... styles) {
            return this.addAll(this.parameterStyles, styles);
         }

         public CommandLine.Help.ColorScheme optionParams(final CommandLine.Help.Ansi.IStyle... styles) {
            return this.addAll(this.optionParamStyles, styles);
         }

         public CommandLine.Help.Ansi.Text commandText(final String command) {
            return this.ansi().apply(command, this.commandStyles);
         }

         public CommandLine.Help.Ansi.Text optionText(final String option) {
            return this.ansi().apply(option, this.optionStyles);
         }

         public CommandLine.Help.Ansi.Text parameterText(final String parameter) {
            return this.ansi().apply(parameter, this.parameterStyles);
         }

         public CommandLine.Help.Ansi.Text optionParamText(final String optionParam) {
            return this.ansi().apply(optionParam, this.optionParamStyles);
         }

         public CommandLine.Help.ColorScheme applySystemProperties() {
            this.replace(this.commandStyles, System.getProperty("picocli.color.commands"));
            this.replace(this.optionStyles, System.getProperty("picocli.color.options"));
            this.replace(this.parameterStyles, System.getProperty("picocli.color.parameters"));
            this.replace(this.optionParamStyles, System.getProperty("picocli.color.optionParams"));
            return this;
         }

         private void replace(final List<CommandLine.Help.Ansi.IStyle> styles, final String property) {
            if (property != null) {
               styles.clear();
               this.addAll(styles, CommandLine.Help.Ansi.Style.parse(property));
            }
         }

         private CommandLine.Help.ColorScheme addAll(final List<CommandLine.Help.Ansi.IStyle> styles, final CommandLine.Help.Ansi.IStyle... add) {
            styles.addAll(Arrays.asList(add));
            return this;
         }

         public CommandLine.Help.Ansi ansi() {
            return this.ansi;
         }
      }

      public static class Column {
         public final int width;
         public final int indent;
         public final CommandLine.Help.Column.Overflow overflow;

         public Column(final int width, final int indent, final CommandLine.Help.Column.Overflow overflow) {
            this.width = width;
            this.indent = indent;
            this.overflow = CommandLine.Assert.notNull(overflow, "overflow");
         }

         public enum Overflow {
            TRUNCATE,
            SPAN,
            WRAP;
         }
      }

      static class DefaultOptionRenderer implements CommandLine.Help.IOptionRenderer {
         public String requiredMarker = " ";
         public Object command;
         private String sep;
         private boolean showDefault;

         @Override
         public CommandLine.Help.Ansi.Text[][] render(
            final CommandLine.Option option,
            final Field field,
            final CommandLine.Help.IParamLabelRenderer paramLabelRenderer,
            final CommandLine.Help.ColorScheme scheme
         ) {
            String[] names = CommandLine.Help.ShortestFirst.sort(option.names());
            int shortOptionCount = names[0].length() == 2 ? 1 : 0;
            String shortOption = shortOptionCount > 0 ? names[0] : "";
            this.sep = shortOptionCount > 0 && names.length > 1 ? "," : "";
            String longOption = CommandLine.Help.join(names, shortOptionCount, names.length - shortOptionCount, ", ");
            CommandLine.Help.Ansi.Text longOptionText = this.createLongOptionText(field, paramLabelRenderer, scheme, longOption);
            this.showDefault = this.command != null && !option.help() && !CommandLine.isBoolean(field.getType());
            Object defaultValue = this.createDefaultValue(field);
            String requiredOption = option.required() ? this.requiredMarker : "";
            return this.renderDescriptionLines(option, scheme, requiredOption, shortOption, longOptionText, defaultValue);
         }

         private Object createDefaultValue(final Field field) {
            Object defaultValue = null;

            try {
               defaultValue = field.get(this.command);
               if (defaultValue == null) {
                  this.showDefault = false;
               } else if (field.getType().isArray()) {
                  StringBuilder sb = new StringBuilder();

                  for (int i = 0; i < Array.getLength(defaultValue); i++) {
                     sb.append(i > 0 ? ", " : "").append(Array.get(defaultValue, i));
                  }

                  defaultValue = sb.insert(0, "[").append("]").toString();
               }
            } catch (Exception ex) {
               this.showDefault = false;
            }

            return defaultValue;
         }

         private CommandLine.Help.Ansi.Text createLongOptionText(
            final Field field, final CommandLine.Help.IParamLabelRenderer renderer, final CommandLine.Help.ColorScheme scheme, final String longOption
         ) {
            CommandLine.Help.Ansi.Text paramLabelText = renderer.renderParameterLabel(field, scheme.ansi(), scheme.optionParamStyles);
            if (paramLabelText.length > 0 && longOption.length() == 0) {
               this.sep = renderer.separator();
               int sepStart = paramLabelText.plainString().indexOf(this.sep);
               CommandLine.Help.Ansi.Text prefix = paramLabelText.substring(0, sepStart);
               paramLabelText = prefix.append(paramLabelText.substring(sepStart + this.sep.length()));
            }

            CommandLine.Help.Ansi.Text longOptionText = scheme.optionText(longOption);
            return longOptionText.append(paramLabelText);
         }

         private CommandLine.Help.Ansi.Text[][] renderDescriptionLines(
            final CommandLine.Option option,
            final CommandLine.Help.ColorScheme scheme,
            final String requiredOption,
            final String shortOption,
            final CommandLine.Help.Ansi.Text longOptionText,
            final Object defaultValue
         ) {
            CommandLine.Help.Ansi.Text EMPTY = CommandLine.Help.Ansi.EMPTY_TEXT;
            List<CommandLine.Help.Ansi.Text[]> result = new ArrayList<>();
            CommandLine.Help.Ansi.Text[] descriptionFirstLines = scheme.ansi().new Text(CommandLine.str(option.description(), 0)).splitLines();
            if (descriptionFirstLines.length == 0) {
               if (this.showDefault) {
                  descriptionFirstLines = new CommandLine.Help.Ansi.Text[]{scheme.ansi().new Text("  Default: " + defaultValue)};
                  this.showDefault = false;
               } else {
                  descriptionFirstLines = new CommandLine.Help.Ansi.Text[]{EMPTY};
               }
            }

            result.add(
               new CommandLine.Help.Ansi.Text[]{
                  scheme.optionText(requiredOption), scheme.optionText(shortOption), scheme.ansi().new Text(this.sep), longOptionText, descriptionFirstLines[0]
               }
            );

            for (int i = 1; i < descriptionFirstLines.length; i++) {
               result.add(new CommandLine.Help.Ansi.Text[]{EMPTY, EMPTY, EMPTY, EMPTY, descriptionFirstLines[i]});
            }

            for (int i = 1; i < option.description().length; i++) {
               CommandLine.Help.Ansi.Text[] descriptionNextLines = scheme.ansi().new Text(option.description()[i]).splitLines();

               for (CommandLine.Help.Ansi.Text line : descriptionNextLines) {
                  result.add(new CommandLine.Help.Ansi.Text[]{EMPTY, EMPTY, EMPTY, EMPTY, line});
               }
            }

            if (this.showDefault) {
               result.add(new CommandLine.Help.Ansi.Text[]{EMPTY, EMPTY, EMPTY, EMPTY, scheme.ansi().new Text("  Default: " + defaultValue)});
            }

            return result.toArray(new CommandLine.Help.Ansi.Text[result.size()][]);
         }
      }

      static class DefaultParamLabelRenderer implements CommandLine.Help.IParamLabelRenderer {
         public final String separator;

         public DefaultParamLabelRenderer(final String separator) {
            this.separator = CommandLine.Assert.notNull(separator, "separator");
         }

         @Override
         public String separator() {
            return this.separator;
         }

         @Override
         public CommandLine.Help.Ansi.Text renderParameterLabel(
            final Field field, final CommandLine.Help.Ansi ansi, final List<CommandLine.Help.Ansi.IStyle> styles
         ) {
            boolean isOptionParameter = field.isAnnotationPresent(CommandLine.Option.class);
            CommandLine.Range arity = isOptionParameter ? CommandLine.Range.optionArity(field) : CommandLine.Range.parameterCapacity(field);
            String split = isOptionParameter
               ? field.getAnnotation(CommandLine.Option.class).split()
               : field.getAnnotation(CommandLine.Parameters.class).split();
            CommandLine.Help.Ansi.Text result = ansi.new Text("");
            String sep = isOptionParameter ? this.separator : "";
            CommandLine.Help.Ansi.Text paramName = ansi.apply(renderParameterName(field), styles);
            if (!CommandLine.empty(split)) {
               paramName = paramName.append("[" + split).append(paramName).append("]...");
            }

            for (int i = 0; i < arity.min; i++) {
               result = result.append(sep).append(paramName);
               sep = " ";
            }

            if (arity.isVariable) {
               if (result.length == 0) {
                  result = result.append(sep + "[").append(paramName).append("]...");
               } else if (!result.plainString().endsWith("...")) {
                  result = result.append("...");
               }
            } else {
               sep = result.length == 0 ? (isOptionParameter ? this.separator : "") : " ";

               for (int i = arity.min; i < arity.max; i++) {
                  if (sep.trim().length() == 0) {
                     result = result.append(sep + "[").append(paramName);
                  } else {
                     result = result.append("[" + sep).append(paramName);
                  }

                  sep = " ";
               }

               for (int i = arity.min; i < arity.max; i++) {
                  result = result.append("]");
               }
            }

            return result;
         }

         private static String renderParameterName(final Field field) {
            String result = null;
            if (field.isAnnotationPresent(CommandLine.Option.class)) {
               result = field.getAnnotation(CommandLine.Option.class).paramLabel();
            } else if (field.isAnnotationPresent(CommandLine.Parameters.class)) {
               result = field.getAnnotation(CommandLine.Parameters.class).paramLabel();
            }

            if (result != null && result.trim().length() > 0) {
               return result.trim();
            }

            String name = field.getName();
            if (Map.class.isAssignableFrom(field.getType())) {
               Class<?>[] paramTypes = CommandLine.getTypeAttribute(field);
               if (paramTypes.length >= 2 && paramTypes[0] != null && paramTypes[1] != null) {
                  name = paramTypes[0].getSimpleName() + "=" + paramTypes[1].getSimpleName();
               } else {
                  name = "String=String";
               }
            }

            return "<" + name + ">";
         }
      }

      static class DefaultParameterRenderer implements CommandLine.Help.IParameterRenderer {
         public String requiredMarker = " ";

         @Override
         public CommandLine.Help.Ansi.Text[][] render(
            final CommandLine.Parameters params,
            final Field field,
            final CommandLine.Help.IParamLabelRenderer paramLabelRenderer,
            final CommandLine.Help.ColorScheme scheme
         ) {
            CommandLine.Help.Ansi.Text label = paramLabelRenderer.renderParameterLabel(field, scheme.ansi(), scheme.parameterStyles);
            CommandLine.Help.Ansi.Text requiredParameter = scheme.parameterText(CommandLine.Range.parameterArity(field).min > 0 ? this.requiredMarker : "");
            CommandLine.Help.Ansi.Text EMPTY = CommandLine.Help.Ansi.EMPTY_TEXT;
            List<CommandLine.Help.Ansi.Text[]> result = new ArrayList<>();
            CommandLine.Help.Ansi.Text[] descriptionFirstLines = scheme.ansi().new Text(CommandLine.str(params.description(), 0)).splitLines();
            if (descriptionFirstLines.length == 0) {
               descriptionFirstLines = new CommandLine.Help.Ansi.Text[]{EMPTY};
            }

            result.add(new CommandLine.Help.Ansi.Text[]{requiredParameter, EMPTY, EMPTY, label, descriptionFirstLines[0]});

            for (int i = 1; i < descriptionFirstLines.length; i++) {
               result.add(new CommandLine.Help.Ansi.Text[]{EMPTY, EMPTY, EMPTY, EMPTY, descriptionFirstLines[i]});
            }

            for (int i = 1; i < params.description().length; i++) {
               CommandLine.Help.Ansi.Text[] descriptionNextLines = scheme.ansi().new Text(params.description()[i]).splitLines();

               for (CommandLine.Help.Ansi.Text line : descriptionNextLines) {
                  result.add(new CommandLine.Help.Ansi.Text[]{EMPTY, EMPTY, EMPTY, EMPTY, line});
               }
            }

            return result.toArray(new CommandLine.Help.Ansi.Text[result.size()][]);
         }
      }

      public interface IOptionRenderer {
         CommandLine.Help.Ansi.Text[][] render(
            CommandLine.Option option, Field field, CommandLine.Help.IParamLabelRenderer parameterLabelRenderer, CommandLine.Help.ColorScheme scheme
         );
      }

      public interface IParamLabelRenderer {
         CommandLine.Help.Ansi.Text renderParameterLabel(Field field, CommandLine.Help.Ansi ansi, List<CommandLine.Help.Ansi.IStyle> styles);

         String separator();
      }

      public interface IParameterRenderer {
         CommandLine.Help.Ansi.Text[][] render(
            CommandLine.Parameters parameters, Field field, CommandLine.Help.IParamLabelRenderer parameterLabelRenderer, CommandLine.Help.ColorScheme scheme
         );
      }

      public static class Layout {
         protected final CommandLine.Help.ColorScheme colorScheme;
         protected final CommandLine.Help.TextTable table;
         protected CommandLine.Help.IOptionRenderer optionRenderer;
         protected CommandLine.Help.IParameterRenderer parameterRenderer;

         public Layout(final CommandLine.Help.ColorScheme colorScheme) {
            this(colorScheme, new CommandLine.Help.TextTable(colorScheme.ansi()));
         }

         public Layout(final CommandLine.Help.ColorScheme colorScheme, final CommandLine.Help.TextTable textTable) {
            this(colorScheme, textTable, new CommandLine.Help.DefaultOptionRenderer(), new CommandLine.Help.DefaultParameterRenderer());
         }

         public Layout(
            final CommandLine.Help.ColorScheme colorScheme,
            final CommandLine.Help.TextTable textTable,
            final CommandLine.Help.IOptionRenderer optionRenderer,
            final CommandLine.Help.IParameterRenderer parameterRenderer
         ) {
            this.colorScheme = CommandLine.Assert.notNull(colorScheme, "colorScheme");
            this.table = CommandLine.Assert.notNull(textTable, "textTable");
            this.optionRenderer = CommandLine.Assert.notNull(optionRenderer, "optionRenderer");
            this.parameterRenderer = CommandLine.Assert.notNull(parameterRenderer, "parameterRenderer");
         }

         public void layout(final Field field, final CommandLine.Help.Ansi.Text[][] cellValues) {
            for (CommandLine.Help.Ansi.Text[] oneRow : cellValues) {
               this.table.addRowValues(oneRow);
            }
         }

         public void addOptions(final List<Field> fields, final CommandLine.Help.IParamLabelRenderer paramLabelRenderer) {
            for (Field field : fields) {
               CommandLine.Option option = field.getAnnotation(CommandLine.Option.class);
               if (!option.hidden()) {
                  this.addOption(field, paramLabelRenderer);
               }
            }
         }

         public void addOption(final Field field, final CommandLine.Help.IParamLabelRenderer paramLabelRenderer) {
            CommandLine.Option option = field.getAnnotation(CommandLine.Option.class);
            CommandLine.Help.Ansi.Text[][] values = this.optionRenderer.render(option, field, paramLabelRenderer, this.colorScheme);
            this.layout(field, values);
         }

         public void addPositionalParameters(final List<Field> fields, final CommandLine.Help.IParamLabelRenderer paramLabelRenderer) {
            for (Field field : fields) {
               CommandLine.Parameters parameters = field.getAnnotation(CommandLine.Parameters.class);
               if (!parameters.hidden()) {
                  this.addPositionalParameter(field, paramLabelRenderer);
               }
            }
         }

         public void addPositionalParameter(final Field field, final CommandLine.Help.IParamLabelRenderer paramLabelRenderer) {
            CommandLine.Parameters option = field.getAnnotation(CommandLine.Parameters.class);
            CommandLine.Help.Ansi.Text[][] values = this.parameterRenderer.render(option, field, paramLabelRenderer, this.colorScheme);
            this.layout(field, values);
         }

         @Override
         public String toString() {
            return this.table.toString();
         }
      }

      static class MinimalOptionRenderer implements CommandLine.Help.IOptionRenderer {
         @Override
         public CommandLine.Help.Ansi.Text[][] render(
            final CommandLine.Option option,
            final Field field,
            final CommandLine.Help.IParamLabelRenderer parameterLabelRenderer,
            final CommandLine.Help.ColorScheme scheme
         ) {
            CommandLine.Help.Ansi.Text optionText = scheme.optionText(option.names()[0]);
            CommandLine.Help.Ansi.Text paramLabelText = parameterLabelRenderer.renderParameterLabel(field, scheme.ansi(), scheme.optionParamStyles);
            optionText = optionText.append(paramLabelText);
            return new CommandLine.Help.Ansi.Text[][]{{optionText, scheme.ansi().new Text(option.description().length == 0 ? "" : option.description()[0])}};
         }
      }

      static class MinimalParameterRenderer implements CommandLine.Help.IParameterRenderer {
         @Override
         public CommandLine.Help.Ansi.Text[][] render(
            final CommandLine.Parameters param,
            final Field field,
            final CommandLine.Help.IParamLabelRenderer parameterLabelRenderer,
            final CommandLine.Help.ColorScheme scheme
         ) {
            return new CommandLine.Help.Ansi.Text[][]{
               {
                     parameterLabelRenderer.renderParameterLabel(field, scheme.ansi(), scheme.parameterStyles),
                     scheme.ansi().new Text(param.description().length == 0 ? "" : param.description()[0])
               }
            };
         }
      }

      static class ShortestFirst implements Comparator<String> {
         public int compare(final String o1, final String o2) {
            return o1.length() - o2.length();
         }

         public static String[] sort(final String[] names) {
            Arrays.sort(names, new CommandLine.Help.ShortestFirst());
            return names;
         }
      }

      static class SortByOptionArityAndNameAlphabetically extends CommandLine.Help.SortByShortestOptionNameAlphabetically {
         @Override
         public int compare(final Field f1, final Field f2) {
            CommandLine.Option o1 = f1.getAnnotation(CommandLine.Option.class);
            CommandLine.Option o2 = f2.getAnnotation(CommandLine.Option.class);
            CommandLine.Range arity1 = CommandLine.Range.optionArity(f1);
            CommandLine.Range arity2 = CommandLine.Range.optionArity(f2);
            int result = arity1.max - arity2.max;
            if (result == 0) {
               result = arity1.min - arity2.min;
            }

            if (result == 0) {
               if (CommandLine.isMultiValue(f1) && !CommandLine.isMultiValue(f2)) {
                  result = 1;
               }

               if (!CommandLine.isMultiValue(f1) && CommandLine.isMultiValue(f2)) {
                  result = -1;
               }
            }

            return result == 0 ? super.compare(f1, f2) : result;
         }
      }

      static class SortByShortestOptionNameAlphabetically implements Comparator<Field> {
         public int compare(final Field f1, final Field f2) {
            CommandLine.Option o1 = f1.getAnnotation(CommandLine.Option.class);
            CommandLine.Option o2 = f2.getAnnotation(CommandLine.Option.class);
            if (o1 == null) {
               return 1;
            }

            if (o2 == null) {
               return -1;
            }

            String[] names1 = CommandLine.Help.ShortestFirst.sort(o1.names());
            String[] names2 = CommandLine.Help.ShortestFirst.sort(o2.names());
            int result = names1[0].toUpperCase().compareTo(names2[0].toUpperCase());
            result = result == 0 ? -names1[0].compareTo(names2[0]) : result;
            return o1.help() == o2.help() ? result : (o2.help() ? -1 : 1);
         }
      }

      public static class TextTable {
         public final CommandLine.Help.Column[] columns;
         protected final List<CommandLine.Help.Ansi.Text> columnValues = new ArrayList<>();
         public int indentWrappedLines = 2;
         private final CommandLine.Help.Ansi ansi;

         public TextTable(final CommandLine.Help.Ansi ansi) {
            this(
               ansi,
               new CommandLine.Help.Column(2, 0, CommandLine.Help.Column.Overflow.TRUNCATE),
               new CommandLine.Help.Column(2, 0, CommandLine.Help.Column.Overflow.TRUNCATE),
               new CommandLine.Help.Column(1, 0, CommandLine.Help.Column.Overflow.TRUNCATE),
               new CommandLine.Help.Column(24, 1, CommandLine.Help.Column.Overflow.SPAN),
               new CommandLine.Help.Column(51, 1, CommandLine.Help.Column.Overflow.WRAP)
            );
         }

         public TextTable(final CommandLine.Help.Ansi ansi, final int... columnWidths) {
            this.ansi = CommandLine.Assert.notNull(ansi, "ansi");
            this.columns = new CommandLine.Help.Column[columnWidths.length];

            for (int i = 0; i < columnWidths.length; i++) {
               this.columns[i] = new CommandLine.Help.Column(
                  columnWidths[i], 0, i == columnWidths.length - 1 ? CommandLine.Help.Column.Overflow.SPAN : CommandLine.Help.Column.Overflow.WRAP
               );
            }
         }

         public TextTable(final CommandLine.Help.Ansi ansi, final CommandLine.Help.Column... columns) {
            this.ansi = CommandLine.Assert.notNull(ansi, "ansi");
            this.columns = CommandLine.Assert.notNull(columns, "columns");
            if (columns.length == 0) {
               throw new IllegalArgumentException("At least one column is required");
            }
         }

         public CommandLine.Help.Ansi.Text textAt(final int row, final int col) {
            return this.columnValues.get(col + row * this.columns.length);
         }

         @Deprecated
         public CommandLine.Help.Ansi.Text cellAt(final int row, final int col) {
            return this.textAt(row, col);
         }

         public int rowCount() {
            return this.columnValues.size() / this.columns.length;
         }

         public void addEmptyRow() {
            for (int i = 0; i < this.columns.length; i++) {
               this.columnValues.add(this.ansi.new Text(this.columns[i].width));
            }
         }

         public void addRowValues(final String... values) {
            CommandLine.Help.Ansi.Text[] array = new CommandLine.Help.Ansi.Text[values.length];

            for (int i = 0; i < array.length; i++) {
               array[i] = values[i] == null ? CommandLine.Help.Ansi.EMPTY_TEXT : this.ansi.new Text(values[i]);
            }

            this.addRowValues(array);
         }

         public void addRowValues(final CommandLine.Help.Ansi.Text... values) {
            if (values.length > this.columns.length) {
               throw new IllegalArgumentException(values.length + " values don't fit in " + this.columns.length + " columns");
            }

            this.addEmptyRow();

            for (int col = 0; col < values.length; col++) {
               int row = this.rowCount() - 1;
               CommandLine.Help.TextTable.Cell cell = this.putValue(row, col, values[col]);
               if ((cell.row != row || cell.column != col) && col != values.length - 1) {
                  this.addEmptyRow();
               }
            }
         }

         public CommandLine.Help.TextTable.Cell putValue(int row, int col, CommandLine.Help.Ansi.Text value) {
            if (row > this.rowCount() - 1) {
               throw new IllegalArgumentException("Cannot write to row " + row + ": rowCount=" + this.rowCount());
            }

            if (value != null && value.plain.length() != 0) {
               CommandLine.Help.Column column = this.columns[col];
               int indent = column.indent;
               switch (column.overflow) {
                  case TRUNCATE:
                     copy(value, this.textAt(row, col), indent);
                     return new CommandLine.Help.TextTable.Cell(col, row);
                  case SPAN:
                     int startColumn = col;

                     do {
                        boolean lastColumn = col == this.columns.length - 1;
                        int charsWritten = lastColumn
                           ? this.copy(BreakIterator.getLineInstance(), value, this.textAt(row, col), indent)
                           : copy(value, this.textAt(row, col), indent);
                        value = value.substring(charsWritten);
                        indent = 0;
                        if (value.length > 0) {
                           col++;
                        }

                        if (value.length > 0 && col >= this.columns.length) {
                           this.addEmptyRow();
                           row++;
                           col = startColumn;
                           indent = column.indent + this.indentWrappedLines;
                        }
                     } while (value.length > 0);

                     return new CommandLine.Help.TextTable.Cell(col, row);
                  case WRAP:
                     BreakIterator lineBreakIterator = BreakIterator.getLineInstance();

                     do {
                        int charsWritten = this.copy(lineBreakIterator, value, this.textAt(row, col), indent);
                        value = value.substring(charsWritten);
                        indent = column.indent + this.indentWrappedLines;
                        if (value.length > 0) {
                           row++;
                           this.addEmptyRow();
                        }
                     } while (value.length > 0);

                     return new CommandLine.Help.TextTable.Cell(col, row);
                  default:
                     throw new IllegalStateException(column.overflow.toString());
               }
            } else {
               return new CommandLine.Help.TextTable.Cell(col, row);
            }
         }

         private static int length(final CommandLine.Help.Ansi.Text str) {
            return str.length;
         }

         private int copy(final BreakIterator line, final CommandLine.Help.Ansi.Text text, final CommandLine.Help.Ansi.Text columnValue, final int offset) {
            line.setText(text.plainString().replace("-", "ÿ"));
            int done = 0;
            int start = line.first();

            for (int end = line.next(); end != -1; end = line.next()) {
               CommandLine.Help.Ansi.Text word = text.substring(start, end);
               if (columnValue.maxLength < offset + done + length(word)) {
                  break;
               }

               done += copy(word, columnValue, offset + done);
               start = end;
            }

            if (done == 0 && length(text) > columnValue.maxLength) {
               done = copy(text, columnValue, offset);
            }

            return done;
         }

         private static int copy(final CommandLine.Help.Ansi.Text value, final CommandLine.Help.Ansi.Text destination, final int offset) {
            int length = Math.min(value.length, destination.maxLength - offset);
            value.getStyledChars(value.from, length, destination, offset);
            return length;
         }

         public StringBuilder toString(final StringBuilder text) {
            int columnCount = this.columns.length;
            StringBuilder row = new StringBuilder(80);

            for (int i = 0; i < this.columnValues.size(); i++) {
               CommandLine.Help.Ansi.Text column = this.columnValues.get(i);
               row.append(column.toString());
               row.append(new String(CommandLine.Help.spaces(this.columns[i % columnCount].width - column.length)));
               if (i % columnCount == columnCount - 1) {
                  int lastChar = row.length() - 1;

                  while (lastChar >= 0 && row.charAt(lastChar) == ' ') {
                     lastChar--;
                  }

                  row.setLength(lastChar + 1);
                  text.append(row.toString()).append(System.getProperty("line.separator"));
                  row.setLength(0);
               }
            }

            return text;
         }

         @Override
         public String toString() {
            return this.toString(new StringBuilder()).toString();
         }

         public static class Cell {
            public final int column;
            public final int row;

            public Cell(final int column, final int row) {
               this.column = column;
               this.row = row;
            }
         }
      }
   }

   public interface IExceptionHandler {
      List<Object> handleException(CommandLine.ParameterException ex, PrintStream out, CommandLine.Help.Ansi ansi, String... args);
   }

   public interface IParseResultHandler {
      List<Object> handleParseResult(List<CommandLine> parsedCommands, PrintStream out, CommandLine.Help.Ansi ansi) throws CommandLine.ExecutionException;
   }

   public interface ITypeConverter<K> {
      K convert(String value) throws Exception;
   }

   public static class InitializationException extends CommandLine.PicocliException {
      private static final long serialVersionUID = 8423014001666638895L;

      public InitializationException(final String msg) {
         super(msg);
      }

      public InitializationException(final String msg, final Exception ex) {
         super(msg, ex);
      }
   }

   private class Interpreter {
      private final Map<String, CommandLine> commands = new LinkedHashMap<>();
      private final Map<Class<?>, CommandLine.ITypeConverter<?>> converterRegistry = new HashMap<>();
      private final Map<String, Field> optionName2Field = new HashMap<>();
      private final Map<Character, Field> singleCharOption2Field = new HashMap<>();
      private final List<Field> requiredFields = new ArrayList<>();
      private final List<Field> positionalParametersFields = new ArrayList<>();
      private final Object command;
      private boolean isHelpRequested;
      private String separator = "=";
      private int position;

      Interpreter(final Object command) {
         this.converterRegistry.put(Path.class, new CommandLine.BuiltIn.PathConverter());
         this.converterRegistry.put(Object.class, new CommandLine.BuiltIn.StringConverter());
         this.converterRegistry.put(String.class, new CommandLine.BuiltIn.StringConverter());
         this.converterRegistry.put(StringBuilder.class, new CommandLine.BuiltIn.StringBuilderConverter());
         this.converterRegistry.put(CharSequence.class, new CommandLine.BuiltIn.CharSequenceConverter());
         this.converterRegistry.put(Byte.class, new CommandLine.BuiltIn.ByteConverter());
         this.converterRegistry.put(byte.class, new CommandLine.BuiltIn.ByteConverter());
         this.converterRegistry.put(Boolean.class, new CommandLine.BuiltIn.BooleanConverter());
         this.converterRegistry.put(boolean.class, new CommandLine.BuiltIn.BooleanConverter());
         this.converterRegistry.put(Character.class, new CommandLine.BuiltIn.CharacterConverter());
         this.converterRegistry.put(char.class, new CommandLine.BuiltIn.CharacterConverter());
         this.converterRegistry.put(Short.class, new CommandLine.BuiltIn.ShortConverter());
         this.converterRegistry.put(short.class, new CommandLine.BuiltIn.ShortConverter());
         this.converterRegistry.put(Integer.class, new CommandLine.BuiltIn.IntegerConverter());
         this.converterRegistry.put(int.class, new CommandLine.BuiltIn.IntegerConverter());
         this.converterRegistry.put(Long.class, new CommandLine.BuiltIn.LongConverter());
         this.converterRegistry.put(long.class, new CommandLine.BuiltIn.LongConverter());
         this.converterRegistry.put(Float.class, new CommandLine.BuiltIn.FloatConverter());
         this.converterRegistry.put(float.class, new CommandLine.BuiltIn.FloatConverter());
         this.converterRegistry.put(Double.class, new CommandLine.BuiltIn.DoubleConverter());
         this.converterRegistry.put(double.class, new CommandLine.BuiltIn.DoubleConverter());
         this.converterRegistry.put(File.class, new CommandLine.BuiltIn.FileConverter());
         this.converterRegistry.put(URI.class, new CommandLine.BuiltIn.URIConverter());
         this.converterRegistry.put(URL.class, new CommandLine.BuiltIn.URLConverter());
         this.converterRegistry.put(Date.class, new CommandLine.BuiltIn.ISO8601DateConverter());
         this.converterRegistry.put(Time.class, new CommandLine.BuiltIn.ISO8601TimeConverter());
         this.converterRegistry.put(BigDecimal.class, new CommandLine.BuiltIn.BigDecimalConverter());
         this.converterRegistry.put(BigInteger.class, new CommandLine.BuiltIn.BigIntegerConverter());
         this.converterRegistry.put(Charset.class, new CommandLine.BuiltIn.CharsetConverter());
         this.converterRegistry.put(InetAddress.class, new CommandLine.BuiltIn.InetAddressConverter());
         this.converterRegistry.put(Pattern.class, new CommandLine.BuiltIn.PatternConverter());
         this.converterRegistry.put(UUID.class, new CommandLine.BuiltIn.UUIDConverter());
         this.command = CommandLine.Assert.notNull(command, "command");
         Class<?> cls = command.getClass();
         String declaredName = null;
         String declaredSeparator = null;
         boolean hasCommandAnnotation = false;

         while (cls != null) {
            CommandLine.init(cls, this.requiredFields, this.optionName2Field, this.singleCharOption2Field, this.positionalParametersFields);
            if (cls.isAnnotationPresent(CommandLine.Command.class)) {
               hasCommandAnnotation = true;
               CommandLine.Command cmd = cls.getAnnotation(CommandLine.Command.class);
               declaredSeparator = declaredSeparator == null ? cmd.separator() : declaredSeparator;
               declaredName = declaredName == null ? cmd.name() : declaredName;
               CommandLine.this.versionLines.addAll(Arrays.asList(cmd.version()));

               for (Class<?> sub : cmd.subcommands()) {
                  CommandLine.Command subCommand = sub.getAnnotation(CommandLine.Command.class);
                  if (subCommand == null || "<main class>".equals(subCommand.name())) {
                     throw new CommandLine.InitializationException(
                        "Subcommand " + sub.getName() + " is missing the mandatory @Command annotation with a 'name' attribute"
                     );
                  }

                  try {
                     Constructor<?> constructor = sub.getDeclaredConstructor();
                     constructor.setAccessible(true);
                     CommandLine commandLine = CommandLine.toCommandLine(constructor.newInstance());
                     commandLine.parent = CommandLine.this;
                     this.commands.put(subCommand.name(), commandLine);
                  } catch (CommandLine.InitializationException ex) {
                     throw ex;
                  } catch (NoSuchMethodException ex) {
                     throw new CommandLine.InitializationException("Cannot instantiate subcommand " + sub.getName() + ": the class has no constructor", ex);
                  } catch (Exception ex) {
                     throw new CommandLine.InitializationException("Could not instantiate and add subcommand " + sub.getName() + ": " + ex, ex);
                  }
               }
            }

            cls = cls.getSuperclass();
         }

         this.separator = declaredSeparator != null ? declaredSeparator : this.separator;
         CommandLine.this.commandName = declaredName != null ? declaredName : CommandLine.this.commandName;
         Collections.sort(this.positionalParametersFields, new CommandLine.PositionalParametersSorter());
         CommandLine.validatePositionalParameters(this.positionalParametersFields);
         if (this.positionalParametersFields.isEmpty() && this.optionName2Field.isEmpty() && !hasCommandAnnotation) {
            throw new CommandLine.InitializationException(
               command + " (" + command.getClass() + ") is not a command: it has no @Command, @Option or @Parameters annotations"
            );
         }
      }

      List<CommandLine> parse(final String... args) {
         CommandLine.Assert.notNull(args, "argument array");
         if (CommandLine.this.tracer.isInfo()) {
            CommandLine.this.tracer.info("Parsing %d command line args %s%n", args.length, Arrays.toString(args));
         }

         Stack<String> arguments = new Stack<>();

         for (int i = args.length - 1; i >= 0; i--) {
            arguments.push(args[i]);
         }

         List<CommandLine> result = new ArrayList<>();
         this.parse(result, arguments, args);
         return result;
      }

      private void parse(final List<CommandLine> parsedCommands, final Stack<String> argumentStack, final String[] originalArgs) {
         this.isHelpRequested = false;
         CommandLine.this.versionHelpRequested = false;
         CommandLine.this.usageHelpRequested = false;
         Class<?> cmdClass = this.command.getClass();
         if (CommandLine.this.tracer.isDebug()) {
            CommandLine.this.tracer
               .debug(
                  "Initializing %s: %d options, %d positional parameters, %d required, %d subcommands.%n",
                  cmdClass.getName(),
                  new HashSet<>(this.optionName2Field.values()).size(),
                  this.positionalParametersFields.size(),
                  this.requiredFields.size(),
                  this.commands.size()
               );
         }

         parsedCommands.add(CommandLine.this);
         List<Field> required = new ArrayList<>(this.requiredFields);
         Set<Field> initialized = new HashSet<>();
         Collections.sort(required, new CommandLine.PositionalParametersSorter());

         try {
            this.processArguments(parsedCommands, argumentStack, required, initialized, originalArgs);
         } catch (CommandLine.ParameterException ex) {
            throw ex;
         } catch (Exception ex) {
            int offendingArgIndex = originalArgs.length - argumentStack.size() - 1;
            String arg = offendingArgIndex >= 0 && offendingArgIndex < originalArgs.length ? originalArgs[offendingArgIndex] : "?";
            throw CommandLine.ParameterException.create(CommandLine.this, ex, arg, offendingArgIndex, originalArgs);
         }

         if (!this.isAnyHelpRequested() && !required.isEmpty()) {
            for (Field missing : required) {
               if (missing.isAnnotationPresent(CommandLine.Option.class)) {
                  throw CommandLine.MissingParameterException.create(CommandLine.this, required, this.separator);
               }

               this.assertNoMissingParameters(missing, CommandLine.Range.parameterArity(missing).min, argumentStack);
            }
         }

         if (!CommandLine.this.unmatchedArguments.isEmpty()) {
            if (!CommandLine.this.isUnmatchedArgumentsAllowed()) {
               throw new CommandLine.UnmatchedArgumentException(CommandLine.this, CommandLine.this.unmatchedArguments);
            }

            if (CommandLine.this.tracer.isWarn()) {
               CommandLine.this.tracer.warn("Unmatched arguments: %s%n", CommandLine.this.unmatchedArguments);
            }
         }
      }

      private void processArguments(
         final List<CommandLine> parsedCommands,
         final Stack<String> args,
         final Collection<Field> required,
         final Set<Field> initialized,
         final String[] originalArgs
      ) throws Exception {
         while (!args.isEmpty()) {
            String arg = args.pop();
            if (CommandLine.this.tracer.isDebug()) {
               CommandLine.this.tracer.debug("Processing argument '%s'. Remainder=%s%n", arg, CommandLine.reverse((Stack)args.clone()));
            }

            if ("--".equals(arg)) {
               CommandLine.this.tracer.info("Found end-of-options delimiter '--'. Treating remainder as positional parameters.%n");
               this.processRemainderAsPositionalParameters(required, initialized, args);
               return;
            }

            if (this.commands.containsKey(arg)) {
               if (!this.isHelpRequested && !required.isEmpty()) {
                  throw CommandLine.MissingParameterException.create(CommandLine.this, required, this.separator);
               }

               if (CommandLine.this.tracer.isDebug()) {
                  CommandLine.this.tracer.debug("Found subcommand '%s' (%s)%n", arg, this.commands.get(arg).interpreter.command.getClass().getName());
               }

               this.commands.get(arg).interpreter.parse(parsedCommands, args, originalArgs);
               return;
            }

            boolean paramAttachedToOption = false;
            int separatorIndex = arg.indexOf(this.separator);
            if (separatorIndex > 0) {
               String key = arg.substring(0, separatorIndex);
               if (this.optionName2Field.containsKey(key) && !this.optionName2Field.containsKey(arg)) {
                  paramAttachedToOption = true;
                  String optionParam = arg.substring(separatorIndex + this.separator.length());
                  args.push(optionParam);
                  arg = key;
                  if (CommandLine.this.tracer.isDebug()) {
                     CommandLine.this.tracer.debug("Separated '%s' option from '%s' option parameter%n", key, optionParam);
                  }
               } else if (CommandLine.this.tracer.isDebug()) {
                  CommandLine.this.tracer.debug("'%s' contains separator '%s' but '%s' is not a known option%n", arg, this.separator, key);
               }
            } else if (CommandLine.this.tracer.isDebug()) {
               CommandLine.this.tracer.debug("'%s' cannot be separated into <option>%s<option-parameter>%n", arg, this.separator);
            }

            if (this.optionName2Field.containsKey(arg)) {
               this.processStandaloneOption(required, initialized, arg, args, paramAttachedToOption);
            } else if (arg.length() > 2 && arg.startsWith("-")) {
               if (CommandLine.this.tracer.isDebug()) {
                  CommandLine.this.tracer.debug("Trying to process '%s' as clustered short options%n", arg, args);
               }

               this.processClusteredShortOptions(required, initialized, arg, args);
            } else {
               args.push(arg);
               if (CommandLine.this.tracer.isDebug()) {
                  CommandLine.this.tracer.debug("Could not find option '%s', deciding whether to treat as unmatched option or positional parameter...%n", arg);
               }

               if (this.resemblesOption(arg)) {
                  this.handleUnmatchedArguments(args.pop());
               } else {
                  if (CommandLine.this.tracer.isDebug()) {
                     CommandLine.this.tracer.debug("No option named '%s' found. Processing remainder as positional parameters%n", arg);
                  }

                  this.processPositionalParameter(required, initialized, args);
               }
            }
         }
      }

      private boolean resemblesOption(final String arg) {
         int count = 0;

         for (String optionName : this.optionName2Field.keySet()) {
            for (int i = 0; i < arg.length() && optionName.length() > i && arg.charAt(i) == optionName.charAt(i); i++) {
               count++;
            }
         }

         boolean result = count > 0 && count * 10 >= this.optionName2Field.size() * 9;
         if (CommandLine.this.tracer.isDebug()) {
            CommandLine.this.tracer
               .debug(
                  "%s %s an option: %d matching prefix chars out of %d option names%n",
                  arg,
                  result ? "resembles" : "doesn't resemble",
                  count,
                  this.optionName2Field.size()
               );
         }

         return result;
      }

      private void handleUnmatchedArguments(final String arg) {
         Stack<String> args = new Stack<>();
         args.add(arg);
         this.handleUnmatchedArguments(args);
      }

      private void handleUnmatchedArguments(final Stack<String> args) {
         while (!args.isEmpty()) {
            CommandLine.this.unmatchedArguments.add(args.pop());
         }
      }

      private void processRemainderAsPositionalParameters(final Collection<Field> required, final Set<Field> initialized, final Stack<String> args) throws Exception {
         while (!args.empty()) {
            this.processPositionalParameter(required, initialized, args);
         }
      }

      private void processPositionalParameter(final Collection<Field> required, final Set<Field> initialized, final Stack<String> args) throws Exception {
         if (CommandLine.this.tracer.isDebug()) {
            CommandLine.this.tracer
               .debug("Processing next arg as a positional parameter at index=%d. Remainder=%s%n", this.position, CommandLine.reverse((Stack)args.clone()));
         }

         int consumed = 0;

         for (Field positionalParam : this.positionalParametersFields) {
            CommandLine.Range indexRange = CommandLine.Range.parameterIndex(positionalParam);
            if (indexRange.contains(this.position)) {
               Stack<String> argsCopy = (Stack<String>)args.clone();
               CommandLine.Range arity = CommandLine.Range.parameterArity(positionalParam);
               if (CommandLine.this.tracer.isDebug()) {
                  CommandLine.this.tracer
                     .debug("Position %d is in index range %s. Trying to assign args to %s, arity=%s%n", this.position, indexRange, positionalParam, arity);
               }

               this.assertNoMissingParameters(positionalParam, arity.min, argsCopy);
               int originalSize = argsCopy.size();
               this.applyOption(
                  positionalParam, CommandLine.Parameters.class, arity, false, argsCopy, initialized, "args[" + indexRange + "] at position " + this.position
               );
               int count = originalSize - argsCopy.size();
               if (count > 0) {
                  required.remove(positionalParam);
               }

               consumed = Math.max(consumed, count);
            }
         }

         for (int i = 0; i < consumed; i++) {
            args.pop();
         }

         this.position += consumed;
         if (CommandLine.this.tracer.isDebug()) {
            CommandLine.this.tracer.debug("Consumed %d arguments, moving position to index %d.%n", consumed, this.position);
         }

         if (consumed == 0 && !args.isEmpty()) {
            this.handleUnmatchedArguments(args.pop());
         }
      }

      private void processStandaloneOption(
         final Collection<Field> required, final Set<Field> initialized, final String arg, final Stack<String> args, final boolean paramAttachedToKey
      ) throws Exception {
         Field field = this.optionName2Field.get(arg);
         required.remove(field);
         CommandLine.Range arity = CommandLine.Range.optionArity(field);
         if (paramAttachedToKey) {
            arity = arity.min(Math.max(1, arity.min));
         }

         if (CommandLine.this.tracer.isDebug()) {
            CommandLine.this.tracer.debug("Found option named '%s': field %s, arity=%s%n", arg, field, arity);
         }

         this.applyOption(field, CommandLine.Option.class, arity, paramAttachedToKey, args, initialized, "option " + arg);
      }

      private void processClusteredShortOptions(final Collection<Field> required, final Set<Field> initialized, final String arg, final Stack<String> args) throws Exception {
         String prefix = arg.substring(0, 1);
         String cluster = arg.substring(1);
         boolean paramAttachedToOption = true;

         while (cluster.length() > 0 && this.singleCharOption2Field.containsKey(cluster.charAt(0))) {
            Field field = this.singleCharOption2Field.get(cluster.charAt(0));
            CommandLine.Range arity = CommandLine.Range.optionArity(field);
            String argDescription = "option " + prefix + cluster.charAt(0);
            if (CommandLine.this.tracer.isDebug()) {
               CommandLine.this.tracer.debug("Found option '%s%s' in %s: field %s, arity=%s%n", prefix, cluster.charAt(0), arg, field, arity);
            }

            required.remove(field);
            cluster = cluster.length() > 0 ? cluster.substring(1) : "";
            paramAttachedToOption = cluster.length() > 0;
            if (cluster.startsWith(this.separator)) {
               cluster = cluster.substring(this.separator.length());
               arity = arity.min(Math.max(1, arity.min));
            }

            if (arity.min > 0 && !CommandLine.empty(cluster) && CommandLine.this.tracer.isDebug()) {
               CommandLine.this.tracer.debug("Trying to process '%s' as option parameter%n", cluster);
            }

            if (!CommandLine.empty(cluster)) {
               args.push(cluster);
            }

            int consumed = this.applyOption(field, CommandLine.Option.class, arity, paramAttachedToOption, args, initialized, argDescription);
            if (CommandLine.empty(cluster) || consumed > 0 || args.isEmpty()) {
               return;
            }

            cluster = args.pop();
         }

         if (cluster.length() != 0) {
            if (arg.endsWith(cluster)) {
               args.push(paramAttachedToOption ? prefix + cluster : cluster);
               if (args.peek().equals(arg)) {
                  if (CommandLine.this.tracer.isDebug()) {
                     CommandLine.this.tracer
                        .debug("Could not match any short options in %s, deciding whether to treat as unmatched option or positional parameter...%n", arg);
                  }

                  if (this.resemblesOption(arg)) {
                     this.handleUnmatchedArguments(args.pop());
                     return;
                  }

                  this.processPositionalParameter(required, initialized, args);
                  return;
               }

               if (CommandLine.this.tracer.isDebug()) {
                  CommandLine.this.tracer.debug("No option found for %s in %s%n", cluster, arg);
               }

               this.handleUnmatchedArguments(args.pop());
            } else {
               args.push(cluster);
               if (CommandLine.this.tracer.isDebug()) {
                  CommandLine.this.tracer.debug("%s is not an option parameter for %s%n", cluster, arg);
               }

               this.processPositionalParameter(required, initialized, args);
            }
         }
      }

      private int applyOption(
         final Field field,
         final Class<?> annotation,
         final CommandLine.Range arity,
         final boolean valueAttachedToOption,
         final Stack<String> args,
         final Set<Field> initialized,
         final String argDescription
      ) throws Exception {
         this.updateHelpRequested(field);
         int length = args.size();
         this.assertNoMissingParameters(field, arity.min, args);
         Class<?> cls = field.getType();
         if (cls.isArray()) {
            return this.applyValuesToArrayField(field, annotation, arity, args, cls, argDescription);
         }

         if (Collection.class.isAssignableFrom(cls)) {
            return this.applyValuesToCollectionField(field, annotation, arity, args, cls, argDescription);
         }

         if (Map.class.isAssignableFrom(cls)) {
            return this.applyValuesToMapField(field, annotation, arity, args, cls, argDescription);
         }

         cls = CommandLine.getTypeAttribute(field)[0];
         return this.applyValueToSingleValuedField(field, arity, args, cls, initialized, argDescription);
      }

      private int applyValueToSingleValuedField(
         final Field field,
         final CommandLine.Range arity,
         final Stack<String> args,
         final Class<?> cls,
         final Set<Field> initialized,
         final String argDescription
      ) throws Exception {
         boolean noMoreValues = args.isEmpty();
         String value = args.isEmpty() ? null : this.trim(args.pop());
         int result = arity.min;
         if ((cls == Boolean.class || cls == boolean.class) && arity.min <= 0) {
            if (arity.max <= 0 || !"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
               if (value != null) {
                  args.push(value);
               }

               Boolean currentValue = (Boolean)field.get(this.command);
               value = String.valueOf(currentValue == null || !currentValue);
            } else {
               result = 1;
            }
         }

         if (noMoreValues && value == null) {
            return 0;
         }

         CommandLine.ITypeConverter<?> converter = this.getTypeConverter(cls, field);
         Object newValue = this.tryConvert(field, -1, converter, value, cls);
         Object oldValue = field.get(this.command);
         CommandLine.TraceLevel level = CommandLine.TraceLevel.INFO;
         String traceMessage = "Setting %s field '%s.%s' to '%5$s' (was '%4$s') for %6$s%n";
         if (initialized != null) {
            if (initialized.contains(field)) {
               if (!CommandLine.this.isOverwrittenOptionsAllowed()) {
                  throw new CommandLine.OverwrittenOptionException(CommandLine.this, this.optionDescription("", field, 0) + " should be specified only once");
               }

               level = CommandLine.TraceLevel.WARN;
               traceMessage = "Overwriting %s field '%s.%s' value '%s' with '%s' for %s%n";
            }

            initialized.add(field);
         }

         if (CommandLine.this.tracer.level.isEnabled(level)) {
            level.print(
               CommandLine.this.tracer,
               traceMessage,
               field.getType().getSimpleName(),
               field.getDeclaringClass().getSimpleName(),
               field.getName(),
               String.valueOf(oldValue),
               String.valueOf(newValue),
               argDescription
            );
         }

         field.set(this.command, newValue);
         return result;
      }

      private int applyValuesToMapField(
         final Field field, final Class<?> annotation, final CommandLine.Range arity, final Stack<String> args, final Class<?> cls, final String argDescription
      ) throws Exception {
         Class<?>[] classes = CommandLine.getTypeAttribute(field);
         if (classes.length < 2) {
            throw new CommandLine.ParameterException(
               CommandLine.this,
               "Field " + field + " needs two types (one for the map key, one for the value) but only has " + classes.length + " types configured."
            );
         }

         CommandLine.ITypeConverter<?> keyConverter = this.getTypeConverter(classes[0], field);
         CommandLine.ITypeConverter<?> valueConverter = this.getTypeConverter(classes[1], field);
         Map<Object, Object> result = (Map<Object, Object>)field.get(this.command);
         if (result == null) {
            result = this.createMap(cls);
            field.set(this.command, result);
         }

         int originalSize = result.size();
         this.consumeMapArguments(field, arity, args, classes, keyConverter, valueConverter, result, argDescription);
         return result.size() - originalSize;
      }

      private void consumeMapArguments(
         final Field field,
         final CommandLine.Range arity,
         final Stack<String> args,
         final Class<?>[] classes,
         final CommandLine.ITypeConverter<?> keyConverter,
         final CommandLine.ITypeConverter<?> valueConverter,
         final Map<Object, Object> result,
         final String argDescription
      ) throws Exception {
         for (int i = 0; i < arity.min; i++) {
            this.consumeOneMapArgument(field, arity, args, classes, keyConverter, valueConverter, result, i, argDescription);
         }

         for (int i = arity.min; i < arity.max && !args.isEmpty(); i++) {
            if (!field.isAnnotationPresent(CommandLine.Parameters.class) && (this.commands.containsKey(args.peek()) || this.isOption(args.peek()))) {
               return;
            }

            this.consumeOneMapArgument(field, arity, args, classes, keyConverter, valueConverter, result, i, argDescription);
         }
      }

      private void consumeOneMapArgument(
         final Field field,
         final CommandLine.Range arity,
         final Stack<String> args,
         final Class<?>[] classes,
         final CommandLine.ITypeConverter<?> keyConverter,
         final CommandLine.ITypeConverter<?> valueConverter,
         final Map<Object, Object> result,
         final int index,
         final String argDescription
      ) throws Exception {
         String[] values = this.split(this.trim(args.pop()), field);

         for (String value : values) {
            String[] keyValue = value.split("=");
            if (keyValue.length < 2) {
               String splitRegex = this.splitRegex(field);
               if (splitRegex.length() == 0) {
                  throw new CommandLine.ParameterException(
                     CommandLine.this, "Value for option " + this.optionDescription("", field, 0) + " should be in KEY=VALUE format but was " + value
                  );
               }

               throw new CommandLine.ParameterException(
                  CommandLine.this,
                  "Value for option "
                     + this.optionDescription("", field, 0)
                     + " should be in KEY=VALUE["
                     + splitRegex
                     + "KEY=VALUE]... format but was "
                     + value
               );
            }

            Object mapKey = this.tryConvert(field, index, keyConverter, keyValue[0], classes[0]);
            Object mapValue = this.tryConvert(field, index, valueConverter, keyValue[1], classes[1]);
            result.put(mapKey, mapValue);
            if (CommandLine.this.tracer.isInfo()) {
               CommandLine.this.tracer
                  .info(
                     "Putting [%s : %s] in %s<%s, %s> field '%s.%s' for %s%n",
                     String.valueOf(mapKey),
                     String.valueOf(mapValue),
                     result.getClass().getSimpleName(),
                     classes[0].getSimpleName(),
                     classes[1].getSimpleName(),
                     field.getDeclaringClass().getSimpleName(),
                     field.getName(),
                     argDescription
                  );
            }
         }
      }

      private void checkMaxArityExceeded(final CommandLine.Range arity, final int remainder, final Field field, final String[] values) {
         if (values.length > remainder) {
            if (arity.max == remainder) {
               remainder + "";
            } else {
               arity + ", remainder=" + remainder;
            }

            throw new CommandLine.MaxValuesforFieldExceededException(
               CommandLine.this,
               this.optionDescription("", field, -1)
                  + " max number of values ("
                  + arity.max
                  + ") exceeded: remainder is "
                  + remainder
                  + " but "
                  + values.length
                  + " values were specified: "
                  + Arrays.toString(values)
            );
         }
      }

      private int applyValuesToArrayField(
         final Field field, final Class<?> annotation, final CommandLine.Range arity, final Stack<String> args, final Class<?> cls, final String argDescription
      ) throws Exception {
         Object existing = field.get(this.command);
         int length = existing == null ? 0 : Array.getLength(existing);
         Class<?> type = CommandLine.getTypeAttribute(field)[0];
         List<Object> converted = this.consumeArguments(field, annotation, arity, args, type, length, argDescription);
         List<Object> newValues = new ArrayList<>();

         for (int i = 0; i < length; i++) {
            newValues.add(Array.get(existing, i));
         }

         for (Object obj : converted) {
            if (obj instanceof Collection) {
               newValues.addAll((Collection<? extends Object>)obj);
            } else {
               newValues.add(obj);
            }
         }

         Object array = Array.newInstance(type, newValues.size());
         field.set(this.command, array);

         for (int i = 0; i < newValues.size(); i++) {
            Array.set(array, i, newValues.get(i));
         }

         return converted.size();
      }

      private int applyValuesToCollectionField(
         final Field field, final Class<?> annotation, final CommandLine.Range arity, final Stack<String> args, final Class<?> cls, final String argDescription
      ) throws Exception {
         Collection<Object> collection = (Collection<Object>)field.get(this.command);
         Class<?> type = CommandLine.getTypeAttribute(field)[0];
         int length = collection == null ? 0 : collection.size();
         List<Object> converted = this.consumeArguments(field, annotation, arity, args, type, length, argDescription);
         if (collection == null) {
            collection = this.createCollection(cls);
            field.set(this.command, collection);
         }

         for (Object element : converted) {
            if (element instanceof Collection) {
               collection.addAll((Collection<? extends Object>)element);
            } else {
               collection.add(element);
            }
         }

         return converted.size();
      }

      private List<Object> consumeArguments(
         final Field field,
         final Class<?> annotation,
         final CommandLine.Range arity,
         final Stack<String> args,
         final Class<?> type,
         final int originalSize,
         final String argDescription
      ) throws Exception {
         List<Object> result = new ArrayList<>();

         for (int i = 0; i < arity.min; i++) {
            this.consumeOneArgument(field, arity, args, type, result, i, originalSize, argDescription);
         }

         for (int i = arity.min; i < arity.max && !args.isEmpty(); i++) {
            if (annotation != CommandLine.Parameters.class && (this.commands.containsKey(args.peek()) || this.isOption(args.peek()))) {
               return result;
            }

            this.consumeOneArgument(field, arity, args, type, result, i, originalSize, argDescription);
         }

         return result;
      }

      private int consumeOneArgument(
         final Field field,
         final CommandLine.Range arity,
         final Stack<String> args,
         final Class<?> type,
         final List<Object> result,
         int index,
         final int originalSize,
         final String argDescription
      ) throws Exception {
         String[] values = this.split(this.trim(args.pop()), field);
         CommandLine.ITypeConverter<?> converter = this.getTypeConverter(type, field);

         for (int j = 0; j < values.length; j++) {
            result.add(this.tryConvert(field, index, converter, values[j], type));
            if (CommandLine.this.tracer.isInfo()) {
               if (field.getType().isArray()) {
                  CommandLine.this.tracer
                     .info(
                        "Adding [%s] to %s[] field '%s.%s' for %s%n",
                        String.valueOf(result.get(result.size() - 1)),
                        type.getSimpleName(),
                        field.getDeclaringClass().getSimpleName(),
                        field.getName(),
                        argDescription
                     );
               } else {
                  CommandLine.this.tracer
                     .info(
                        "Adding [%s] to %s<%s> field '%s.%s' for %s%n",
                        String.valueOf(result.get(result.size() - 1)),
                        field.getType().getSimpleName(),
                        type.getSimpleName(),
                        field.getDeclaringClass().getSimpleName(),
                        field.getName(),
                        argDescription
                     );
               }
            }
         }

         return index + 1;
      }

      private String splitRegex(final Field field) {
         if (field.isAnnotationPresent(CommandLine.Option.class)) {
            return field.getAnnotation(CommandLine.Option.class).split();
         } else {
            return field.isAnnotationPresent(CommandLine.Parameters.class) ? field.getAnnotation(CommandLine.Parameters.class).split() : "";
         }
      }

      private String[] split(final String value, final Field field) {
         String regex = this.splitRegex(field);
         return regex.length() == 0 ? new String[]{value} : value.split(regex);
      }

      private boolean isOption(final String arg) {
         if ("--".equals(arg)) {
            return true;
         }

         if (this.optionName2Field.containsKey(arg)) {
            return true;
         }

         int separatorIndex = arg.indexOf(this.separator);
         return separatorIndex > 0 && this.optionName2Field.containsKey(arg.substring(0, separatorIndex))
            ? true
            : arg.length() > 2 && arg.startsWith("-") && this.singleCharOption2Field.containsKey(arg.charAt(1));
      }

      private Object tryConvert(final Field field, final int index, final CommandLine.ITypeConverter<?> converter, final String value, final Class<?> type) throws Exception {
         try {
            return converter.convert(value);
         } catch (CommandLine.TypeConversionException ex) {
            throw new CommandLine.ParameterException(CommandLine.this, ex.getMessage() + this.optionDescription(" for ", field, index));
         } catch (Exception other) {
            String desc = this.optionDescription(" for ", field, index) + ": " + other;
            throw new CommandLine.ParameterException(CommandLine.this, "Could not convert '" + value + "' to " + type.getSimpleName() + desc, other);
         }
      }

      private String optionDescription(final String prefix, final Field field, final int index) {
         CommandLine.Help.IParamLabelRenderer labelRenderer = CommandLine.Help.createMinimalParamLabelRenderer();
         String desc = "";
         if (field.isAnnotationPresent(CommandLine.Option.class)) {
            desc = prefix + "option '" + field.getAnnotation(CommandLine.Option.class).names()[0] + "'";
            if (index >= 0) {
               CommandLine.Range arity = CommandLine.Range.optionArity(field);
               if (arity.max > 1) {
                  desc = desc + " at index " + index;
               }

               desc = desc + " (" + labelRenderer.renderParameterLabel(field, CommandLine.Help.Ansi.OFF, Collections.emptyList()) + ")";
            }
         } else if (field.isAnnotationPresent(CommandLine.Parameters.class)) {
            CommandLine.Range indexRange = CommandLine.Range.parameterIndex(field);
            CommandLine.Help.Ansi.Text label = labelRenderer.renderParameterLabel(field, CommandLine.Help.Ansi.OFF, Collections.emptyList());
            desc = prefix + "positional parameter at index " + indexRange + " (" + label + ")";
         }

         return desc;
      }

      private boolean isAnyHelpRequested() {
         return this.isHelpRequested || CommandLine.this.versionHelpRequested || CommandLine.this.usageHelpRequested;
      }

      private void updateHelpRequested(final Field field) {
         if (field.isAnnotationPresent(CommandLine.Option.class)) {
            this.isHelpRequested = this.isHelpRequested | this.is(field, "help", field.getAnnotation(CommandLine.Option.class).help());
            CommandLine.this.versionHelpRequested = CommandLine.this.versionHelpRequested
               | this.is(field, "versionHelp", field.getAnnotation(CommandLine.Option.class).versionHelp());
            CommandLine.this.usageHelpRequested = CommandLine.this.usageHelpRequested
               | this.is(field, "usageHelp", field.getAnnotation(CommandLine.Option.class).usageHelp());
         }
      }

      private boolean is(final Field f, final String description, final boolean value) {
         if (value && CommandLine.this.tracer.isInfo()) {
            CommandLine.this.tracer
               .info("Field '%s.%s' has '%s' annotation: not validating required fields%n", f.getDeclaringClass().getSimpleName(), f.getName(), description);
         }

         return value;
      }

      private Collection<Object> createCollection(final Class<?> collectionClass) throws Exception {
         if (collectionClass.isInterface()) {
            if (List.class.isAssignableFrom(collectionClass)) {
               return new ArrayList<>();
            } else if (SortedSet.class.isAssignableFrom(collectionClass)) {
               return new TreeSet<>();
            } else if (Set.class.isAssignableFrom(collectionClass)) {
               return new LinkedHashSet<>();
            } else {
               return Queue.class.isAssignableFrom(collectionClass) ? new LinkedList<>() : new ArrayList<>();
            }
         } else {
            return (Collection<Object>)collectionClass.newInstance();
         }
      }

      private Map<Object, Object> createMap(final Class<?> mapClass) throws Exception {
         try {
            return (Map<Object, Object>)mapClass.newInstance();
         } catch (Exception var3) {
            return new LinkedHashMap<>();
         }
      }

      private CommandLine.ITypeConverter<?> getTypeConverter(final Class<?> type, final Field field) {
         CommandLine.ITypeConverter<?> result = this.converterRegistry.get(type);
         if (result != null) {
            return result;
         } else if (type.isEnum()) {
            return value -> Enum.valueOf(type, value);
         } else {
            throw new CommandLine.MissingTypeConverterException(CommandLine.this, "No TypeConverter registered for " + type.getName() + " of field " + field);
         }
      }

      private void assertNoMissingParameters(final Field field, final int arity, final Stack<String> args) {
         if (arity > args.size()) {
            if (arity == 1) {
               if (field.isAnnotationPresent(CommandLine.Option.class)) {
                  throw new CommandLine.MissingParameterException(CommandLine.this, "Missing required parameter for " + this.optionDescription("", field, 0));
               }

               CommandLine.Range indexRange = CommandLine.Range.parameterIndex(field);
               CommandLine.Help.IParamLabelRenderer labelRenderer = CommandLine.Help.createMinimalParamLabelRenderer();
               String sep = "";
               StringBuilder names = new StringBuilder();
               int count = 0;

               for (int i = indexRange.min; i < this.positionalParametersFields.size(); i++) {
                  if (CommandLine.Range.parameterArity(this.positionalParametersFields.get(i)).min > 0) {
                     names.append(sep)
                        .append(labelRenderer.renderParameterLabel(this.positionalParametersFields.get(i), CommandLine.Help.Ansi.OFF, Collections.emptyList()));
                     sep = ", ";
                     count++;
                  }
               }

               String msg = "Missing required parameter";
               CommandLine.Range paramArity = CommandLine.Range.parameterArity(field);
               if (paramArity.isVariable) {
                  msg = msg + "s at positions " + indexRange + ": ";
               } else {
                  msg = msg + (count > 1 ? "s: " : ": ");
               }

               throw new CommandLine.MissingParameterException(CommandLine.this, msg + names);
            } else if (args.isEmpty()) {
               throw new CommandLine.MissingParameterException(
                  CommandLine.this, this.optionDescription("", field, 0) + " requires at least " + arity + " values, but none were specified."
               );
            } else {
               throw new CommandLine.MissingParameterException(
                  CommandLine.this,
                  this.optionDescription("", field, 0)
                     + " requires at least "
                     + arity
                     + " values, but only "
                     + args.size()
                     + " were specified: "
                     + CommandLine.<String>reverse(args)
               );
            }
         }
      }

      private String trim(final String value) {
         return this.unquote(value);
      }

      private String unquote(final String value) {
         return value == null ? null : (value.length() > 1 && value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value);
      }
   }

   public static class MaxValuesforFieldExceededException extends CommandLine.ParameterException {
      private static final long serialVersionUID = 6536145439570100641L;

      public MaxValuesforFieldExceededException(final CommandLine commandLine, final String msg) {
         super(commandLine, msg);
      }
   }

   public static class MissingParameterException extends CommandLine.ParameterException {
      private static final long serialVersionUID = 5075678535706338753L;

      public MissingParameterException(final CommandLine commandLine, final String msg) {
         super(commandLine, msg);
      }

      private static CommandLine.MissingParameterException create(final CommandLine cmd, final Collection<Field> missing, final String separator) {
         if (missing.size() == 1) {
            return new CommandLine.MissingParameterException(cmd, "Missing required option '" + describe(missing.iterator().next(), separator) + "'");
         }

         List<String> names = new ArrayList<>(missing.size());

         for (Field field : missing) {
            names.add(describe(field, separator));
         }

         return new CommandLine.MissingParameterException(cmd, "Missing required options " + names.toString());
      }

      private static String describe(final Field field, final String separator) {
         String prefix = field.isAnnotationPresent(CommandLine.Option.class)
            ? field.getAnnotation(CommandLine.Option.class).names()[0] + separator
            : "params[" + field.getAnnotation(CommandLine.Parameters.class).index() + "]" + separator;
         return prefix + CommandLine.Help.DefaultParamLabelRenderer.renderParameterName(field);
      }
   }

   public static class MissingTypeConverterException extends CommandLine.ParameterException {
      private static final long serialVersionUID = -6050931703233083760L;

      public MissingTypeConverterException(final CommandLine commandLine, final String msg) {
         super(commandLine, msg);
      }
   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target(ElementType.FIELD)
   public @interface Option {
      String[] names();

      boolean required() default false;

      /** @deprecated */
      boolean help() default false;

      boolean usageHelp() default false;

      boolean versionHelp() default false;

      String[] description() default {};

      String arity() default "";

      String paramLabel() default "";

      Class<?>[] type() default {};

      String split() default "";

      boolean hidden() default false;
   }

   public static class OverwrittenOptionException extends CommandLine.ParameterException {
      private static final long serialVersionUID = 1338029208271055776L;

      public OverwrittenOptionException(final CommandLine commandLine, final String msg) {
         super(commandLine, msg);
      }
   }

   public static class ParameterException extends CommandLine.PicocliException {
      private static final long serialVersionUID = 1477112829129763139L;
      private final CommandLine commandLine;

      public ParameterException(final CommandLine commandLine, final String msg) {
         super(msg);
         this.commandLine = CommandLine.Assert.notNull(commandLine, "commandLine");
      }

      public ParameterException(final CommandLine commandLine, final String msg, final Exception ex) {
         super(msg, ex);
         this.commandLine = CommandLine.Assert.notNull(commandLine, "commandLine");
      }

      public CommandLine getCommandLine() {
         return this.commandLine;
      }

      private static CommandLine.ParameterException create(final CommandLine cmd, final Exception ex, final String arg, final int i, final String[] args) {
         String msg = ex.getClass().getSimpleName()
            + ": "
            + ex.getLocalizedMessage()
            + " while processing argument at or before arg["
            + i
            + "] '"
            + arg
            + "' in "
            + Arrays.toString(args)
            + ": "
            + ex.toString();
         return new CommandLine.ParameterException(cmd, msg, ex);
      }
   }

   public static class ParameterIndexGapException extends CommandLine.InitializationException {
      private static final long serialVersionUID = -1520981133257618319L;

      public ParameterIndexGapException(final String msg) {
         super(msg);
      }
   }

   @Retention(RetentionPolicy.RUNTIME)
   @Target(ElementType.FIELD)
   public @interface Parameters {
      String index() default "*";

      String[] description() default {};

      String arity() default "";

      String paramLabel() default "";

      Class<?>[] type() default {};

      String split() default "";

      boolean hidden() default false;
   }

   public static class PicocliException extends RuntimeException {
      private static final long serialVersionUID = -2574128880125050818L;

      public PicocliException(final String msg) {
         super(msg);
      }

      public PicocliException(final String msg, final Exception ex) {
         super(msg, ex);
      }
   }

   private static class PositionalParametersSorter implements Comparator<Field> {
      public int compare(final Field o1, final Field o2) {
         int result = CommandLine.Range.parameterIndex(o1).compareTo(CommandLine.Range.parameterIndex(o2));
         return result == 0 ? CommandLine.Range.parameterArity(o1).compareTo(CommandLine.Range.parameterArity(o2)) : result;
      }
   }

   public static class Range implements Comparable<CommandLine.Range> {
      public final int min;
      public final int max;
      public final boolean isVariable;
      private final boolean isUnspecified;
      private final String originalValue;

      public Range(final int min, final int max, final boolean variable, final boolean unspecified, final String originalValue) {
         this.min = min;
         this.max = max;
         this.isVariable = variable;
         this.isUnspecified = unspecified;
         this.originalValue = originalValue;
      }

      public static CommandLine.Range optionArity(final Field field) {
         return field.isAnnotationPresent(CommandLine.Option.class)
            ? adjustForType(valueOf(field.getAnnotation(CommandLine.Option.class).arity()), field)
            : new CommandLine.Range(0, 0, false, true, "0");
      }

      public static CommandLine.Range parameterArity(final Field field) {
         return field.isAnnotationPresent(CommandLine.Parameters.class)
            ? adjustForType(valueOf(field.getAnnotation(CommandLine.Parameters.class).arity()), field)
            : new CommandLine.Range(0, 0, false, true, "0");
      }

      public static CommandLine.Range parameterIndex(final Field field) {
         return field.isAnnotationPresent(CommandLine.Parameters.class)
            ? valueOf(field.getAnnotation(CommandLine.Parameters.class).index())
            : new CommandLine.Range(0, 0, false, true, "0");
      }

      static CommandLine.Range adjustForType(final CommandLine.Range result, final Field field) {
         return result.isUnspecified ? defaultArity(field) : result;
      }

      public static CommandLine.Range defaultArity(final Field field) {
         Class<?> type = field.getType();
         if (field.isAnnotationPresent(CommandLine.Option.class)) {
            return defaultArity(type);
         } else {
            return CommandLine.isMultiValue(type) ? valueOf("0..1") : valueOf("1");
         }
      }

      public static CommandLine.Range defaultArity(final Class<?> type) {
         return CommandLine.isBoolean(type) ? valueOf("0") : valueOf("1");
      }

      private int size() {
         return 1 + this.max - this.min;
      }

      static CommandLine.Range parameterCapacity(final Field field) {
         CommandLine.Range arity = parameterArity(field);
         if (!CommandLine.isMultiValue(field)) {
            return arity;
         } else {
            CommandLine.Range index = parameterIndex(field);
            if (arity.max == 0) {
               return arity;
            } else if (index.size() == 1) {
               return arity;
            } else if (index.isVariable) {
               return valueOf(arity.min + "..*");
            } else if (arity.size() == 1) {
               return valueOf(arity.min * index.size() + "");
            } else {
               return arity.isVariable ? valueOf(arity.min * index.size() + "..*") : valueOf(arity.min * index.size() + ".." + arity.max * index.size());
            }
         }
      }

      public static CommandLine.Range valueOf(String range) {
         range = range.trim();
         boolean unspecified = range.length() == 0 || range.startsWith("..");
         int min = -1;
         int max = -1;
         boolean variable = false;
         int dots = -1;
         if ((dots = range.indexOf("..")) >= 0) {
            min = parseInt(range.substring(0, dots), 0);
            max = parseInt(range.substring(dots + 2), Integer.MAX_VALUE);
            variable = max == Integer.MAX_VALUE;
         } else {
            max = parseInt(range, Integer.MAX_VALUE);
            variable = max == Integer.MAX_VALUE;
            min = variable ? 0 : max;
         }

         return new CommandLine.Range(min, max, variable, unspecified, range);
      }

      private static int parseInt(final String str, final int defaultValue) {
         try {
            return Integer.parseInt(str);
         } catch (Exception ex) {
            return defaultValue;
         }
      }

      public CommandLine.Range min(final int newMin) {
         return new CommandLine.Range(newMin, Math.max(newMin, this.max), this.isVariable, this.isUnspecified, this.originalValue);
      }

      public CommandLine.Range max(final int newMax) {
         return new CommandLine.Range(Math.min(this.min, newMax), newMax, this.isVariable, this.isUnspecified, this.originalValue);
      }

      public boolean contains(final int value) {
         return this.min <= value && this.max >= value;
      }

      @Override
      public boolean equals(final Object object) {
         if (!(object instanceof CommandLine.Range)) {
            return false;
         }

         CommandLine.Range other = (CommandLine.Range)object;
         return other.max == this.max && other.min == this.min && other.isVariable == this.isVariable;
      }

      @Override
      public int hashCode() {
         return ((629 + this.max) * 37 + this.min) * 37 + (this.isVariable ? 1 : 0);
      }

      @Override
      public String toString() {
         return this.min == this.max ? String.valueOf(this.min) : this.min + ".." + (this.isVariable ? "*" : this.max);
      }

      public int compareTo(final CommandLine.Range other) {
         int result = this.min - other.min;
         return result == 0 ? this.max - other.max : result;
      }
   }

   public static class RunAll implements CommandLine.IParseResultHandler {
      @Override
      public List<Object> handleParseResult(final List<CommandLine> parsedCommands, final PrintStream out, final CommandLine.Help.Ansi ansi) {
         if (CommandLine.printHelpIfRequested(parsedCommands, out, ansi)) {
            return null;
         }

         List<Object> result = new ArrayList<>();

         for (CommandLine parsed : parsedCommands) {
            result.add(CommandLine.execute(parsed));
         }

         return result;
      }
   }

   public static class RunFirst implements CommandLine.IParseResultHandler {
      @Override
      public List<Object> handleParseResult(final List<CommandLine> parsedCommands, final PrintStream out, final CommandLine.Help.Ansi ansi) {
         return CommandLine.printHelpIfRequested(parsedCommands, out, ansi)
            ? Collections.emptyList()
            : Arrays.asList(CommandLine.execute(parsedCommands.get(0)));
      }
   }

   public static class RunLast implements CommandLine.IParseResultHandler {
      @Override
      public List<Object> handleParseResult(final List<CommandLine> parsedCommands, final PrintStream out, final CommandLine.Help.Ansi ansi) {
         if (CommandLine.printHelpIfRequested(parsedCommands, out, ansi)) {
            return Collections.emptyList();
         }

         CommandLine last = parsedCommands.get(parsedCommands.size() - 1);
         return Arrays.asList(CommandLine.execute(last));
      }
   }

   private enum TraceLevel {
      OFF,
      WARN,
      INFO,
      DEBUG;

      public boolean isEnabled(final CommandLine.TraceLevel other) {
         return this.ordinal() >= other.ordinal();
      }

      private void print(final CommandLine.Tracer tracer, final String msg, final Object... params) {
         if (tracer.level.isEnabled(this)) {
            tracer.stream.printf(this.prefix(msg), params);
         }
      }

      private String prefix(final String msg) {
         return "[picocli " + this + "] " + msg;
      }

      static CommandLine.TraceLevel lookup(final String key) {
         return key == null ? WARN : (!CommandLine.empty(key) && !"true".equalsIgnoreCase(key) ? valueOf(key) : INFO);
      }
   }

   private static class Tracer {
      private final CommandLine.TraceLevel level = CommandLine.TraceLevel.lookup(System.getProperty("picocli.trace"));
      private final PrintStream stream = System.err;

      void warn(final String msg, final Object... params) {
         CommandLine.TraceLevel.WARN.print(this, msg, params);
      }

      void info(final String msg, final Object... params) {
         CommandLine.TraceLevel.INFO.print(this, msg, params);
      }

      void debug(final String msg, final Object... params) {
         CommandLine.TraceLevel.DEBUG.print(this, msg, params);
      }

      boolean isWarn() {
         return this.level.isEnabled(CommandLine.TraceLevel.WARN);
      }

      boolean isInfo() {
         return this.level.isEnabled(CommandLine.TraceLevel.INFO);
      }

      boolean isDebug() {
         return this.level.isEnabled(CommandLine.TraceLevel.DEBUG);
      }
   }

   public static class TypeConversionException extends CommandLine.PicocliException {
      private static final long serialVersionUID = 4251973913816346114L;

      public TypeConversionException(final String msg) {
         super(msg);
      }
   }

   public static class UnmatchedArgumentException extends CommandLine.ParameterException {
      private static final long serialVersionUID = -8700426380701452440L;

      public UnmatchedArgumentException(final CommandLine commandLine, final String msg) {
         super(commandLine, msg);
      }

      public UnmatchedArgumentException(final CommandLine commandLine, final Stack<String> args) {
         this(commandLine, new ArrayList<>(CommandLine.reverse(args)));
      }

      public UnmatchedArgumentException(final CommandLine commandLine, final List<String> args) {
         this(commandLine, "Unmatched argument" + (args.size() == 1 ? " " : "s ") + args);
      }
   }
}
