package org.apache.logging.log4j.core.pattern;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.time.SystemNanoClock;
import org.apache.logging.log4j.plugins.di.DI;
import org.apache.logging.log4j.plugins.di.Key;
import org.apache.logging.log4j.plugins.model.PluginNamespace;
import org.apache.logging.log4j.plugins.model.PluginType;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

public final class PatternParser {
   static final String DISABLE_ANSI = "disableAnsi";
   static final String NO_CONSOLE_NO_ANSI = "noConsoleNoAnsi";
   private static final char ESCAPE_CHAR = '%';
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final int BUF_SIZE = 32;
   private static final int DECIMAL = 10;
   private static final Key<PluginNamespace> PLUGIN_CATEGORY_KEY = Key.forClass(PluginNamespace.class);
   private final Configuration config;
   private final Map<String, Class<? extends PatternConverter>> converterRules;

   public PatternParser(final String converterKey) {
      this(null, converterKey, null, null);
   }

   public PatternParser(final Configuration config, final String converterKey, final Class<?> expected) {
      this(config, converterKey, expected, null);
   }

   public PatternParser(final Configuration config, final String converterKey, final Class<?> expectedClass, final Class<?> filterClass) {
      this.config = config;
      Key<PluginNamespace> pluginCategoryKey = PLUGIN_CATEGORY_KEY.withNamespace(converterKey);
      PluginNamespace plugins;
      if (config == null) {
         plugins = (PluginNamespace)DI.createInjector().getInstance(pluginCategoryKey);
      } else {
         plugins = config.getComponent(pluginCategoryKey);
      }

      Map<String, Class<? extends PatternConverter>> converters = new LinkedHashMap<>();

      for (PluginType<?> type : plugins) {
         try {
            Class<? extends PatternConverter> clazz = type.getPluginClass().asSubclass(PatternConverter.class);
            if (filterClass == null || filterClass.isAssignableFrom(clazz)) {
               ConverterKeys keys = clazz.getAnnotation(ConverterKeys.class);
               if (keys != null) {
                  for (String key : keys.value()) {
                     if (converters.containsKey(key)) {
                        LOGGER.warn(
                           "Converter key '{}' is already mapped to '{}'. Sorry, Dave, I can't let you do that! Ignoring plugin [{}].",
                           key,
                           converters.get(key),
                           clazz
                        );
                     } else {
                        converters.put(key, clazz);
                     }
                  }
               }
            }
         } catch (Exception ex) {
            LOGGER.error("Error processing plugin " + type.getElementType(), ex);
         }
      }

      this.converterRules = converters;
   }

   public List<PatternFormatter> parse(final String pattern) {
      return this.parse(pattern, false, false, false);
   }

   public List<PatternFormatter> parse(final String pattern, final boolean alwaysWriteExceptions, final boolean noConsoleNoAnsi) {
      return this.parse(pattern, alwaysWriteExceptions, false, noConsoleNoAnsi);
   }

   public List<PatternFormatter> parse(final String pattern, final boolean alwaysWriteExceptions, final boolean disableAnsi, final boolean noConsoleNoAnsi) {
      List<PatternFormatter> list = new ArrayList<>();
      List<PatternConverter> converters = new ArrayList<>();
      List<FormattingInfo> fields = new ArrayList<>();
      this.parse(pattern, converters, fields, disableAnsi, noConsoleNoAnsi, true);
      Iterator<FormattingInfo> fieldIter = fields.iterator();
      boolean handlesThrowable = false;

      for (PatternConverter converter : converters) {
         if (converter instanceof NanoTimePatternConverter && this.config != null) {
            this.config.setNanoClock(new SystemNanoClock());
         }

         LogEventPatternConverter pc;
         if (converter instanceof LogEventPatternConverter) {
            pc = (LogEventPatternConverter)converter;
            handlesThrowable |= pc.handlesThrowable();
         } else {
            pc = SimpleLiteralPatternConverter.of("");
         }

         FormattingInfo field;
         if (fieldIter.hasNext()) {
            field = fieldIter.next();
         } else {
            field = FormattingInfo.getDefault();
         }

         list.add(new PatternFormatter(pc, field));
      }

      if (alwaysWriteExceptions && !handlesThrowable) {
         LogEventPatternConverter pc = ExtendedThrowablePatternConverter.newInstance(this.config, null);
         list.add(new PatternFormatter(pc, FormattingInfo.getDefault()));
      }

      return list;
   }

   private static int extractConverter(
      final char lastChar, final String pattern, final int start, final StringBuilder convBuf, final StringBuilder currentLiteral
   ) {
      int i = start;
      convBuf.setLength(0);
      if (!Character.isUnicodeIdentifierStart(lastChar)) {
         return i;
      }

      convBuf.append(lastChar);

      while (i < pattern.length() && Character.isUnicodeIdentifierPart(pattern.charAt(i))) {
         convBuf.append(pattern.charAt(i));
         currentLiteral.append(pattern.charAt(i));
         i++;
      }

      return i;
   }

   private static int extractOptions(final String pattern, final int start, final List<String> options) {
      int i = start;

      while (i < pattern.length() && pattern.charAt(i) == '{') {
         int begin = ++i;

         int depth;
         for (depth = 1; depth > 0 && i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '{') {
               depth++;
            } else if (c == '}') {
               depth--;
            }
         }

         if (depth > 0) {
            i = pattern.lastIndexOf(125);
            if (i != -1 && i >= start) {
               return i + 1;
            }

            return begin;
         }

         options.add(pattern.substring(begin, i - 1));
      }

      return i;
   }

   public void parse(
      final String pattern,
      final List<PatternConverter> patternConverters,
      final List<FormattingInfo> formattingInfos,
      final boolean noConsoleNoAnsi,
      final boolean convertBackslashes
   ) {
      this.parse(pattern, patternConverters, formattingInfos, false, noConsoleNoAnsi, convertBackslashes);
   }

   public void parse(
      final String pattern,
      final List<PatternConverter> patternConverters,
      final List<FormattingInfo> formattingInfos,
      final boolean disableAnsi,
      final boolean noConsoleNoAnsi,
      final boolean convertBackslashes
   ) {
      Objects.requireNonNull(pattern, "pattern");
      StringBuilder currentLiteral = new StringBuilder(32);
      int patternLength = pattern.length();
      PatternParser.ParserState state = PatternParser.ParserState.LITERAL_STATE;
      int i = 0;
      FormattingInfo formattingInfo = FormattingInfo.getDefault();

      while (i < patternLength) {
         char c = pattern.charAt(i++);
         switch (state) {
            case LITERAL_STATE:
               if (i == patternLength) {
                  currentLiteral.append(c);
               } else if (c == '%') {
                  switch (pattern.charAt(i)) {
                     case '%':
                        currentLiteral.append(c);
                        i++;
                        break;
                     default:
                        if (currentLiteral.length() != 0) {
                           patternConverters.add(this.literalPattern(currentLiteral.toString(), convertBackslashes));
                           formattingInfos.add(FormattingInfo.getDefault());
                        }

                        currentLiteral.setLength(0);
                        currentLiteral.append(c);
                        state = PatternParser.ParserState.CONVERTER_STATE;
                        formattingInfo = FormattingInfo.getDefault();
                  }
               } else {
                  currentLiteral.append(c);
               }
               break;
            case CONVERTER_STATE:
               currentLiteral.append(c);
               switch (c) {
                  case '-':
                     formattingInfo = new FormattingInfo(
                        true, formattingInfo.getMinLength(), formattingInfo.getMaxLength(), formattingInfo.isLeftTruncate(), formattingInfo.isZeroPad()
                     );
                     continue;
                  case '.':
                     state = PatternParser.ParserState.DOT_STATE;
                     continue;
                  case '/':
                  default:
                     if (c >= '0' && c <= '9') {
                        formattingInfo = new FormattingInfo(
                           formattingInfo.isLeftAligned(), c - '0', formattingInfo.getMaxLength(), formattingInfo.isLeftTruncate(), formattingInfo.isZeroPad()
                        );
                        state = PatternParser.ParserState.MIN_STATE;
                        continue;
                     }

                     i = this.finalizeConverter(
                        c,
                        pattern,
                        i,
                        currentLiteral,
                        formattingInfo,
                        this.converterRules,
                        patternConverters,
                        formattingInfos,
                        disableAnsi,
                        noConsoleNoAnsi,
                        convertBackslashes
                     );
                     state = PatternParser.ParserState.LITERAL_STATE;
                     formattingInfo = FormattingInfo.getDefault();
                     currentLiteral.setLength(0);
                     continue;
                  case '0':
                     formattingInfo = new FormattingInfo(
                        formattingInfo.isLeftAligned(), formattingInfo.getMinLength(), formattingInfo.getMaxLength(), formattingInfo.isLeftTruncate(), true
                     );
                     continue;
               }
            case MIN_STATE:
               currentLiteral.append(c);
               if (c >= '0' && c <= '9') {
                  formattingInfo = new FormattingInfo(
                     formattingInfo.isLeftAligned(),
                     formattingInfo.getMinLength() * 10 + c - 48,
                     formattingInfo.getMaxLength(),
                     formattingInfo.isLeftTruncate(),
                     formattingInfo.isZeroPad()
                  );
               } else if (c == '.') {
                  state = PatternParser.ParserState.DOT_STATE;
               } else {
                  i = this.finalizeConverter(
                     c,
                     pattern,
                     i,
                     currentLiteral,
                     formattingInfo,
                     this.converterRules,
                     patternConverters,
                     formattingInfos,
                     disableAnsi,
                     noConsoleNoAnsi,
                     convertBackslashes
                  );
                  state = PatternParser.ParserState.LITERAL_STATE;
                  formattingInfo = FormattingInfo.getDefault();
                  currentLiteral.setLength(0);
               }
               break;
            case DOT_STATE:
               currentLiteral.append(c);
               switch (c) {
                  case '-':
                     formattingInfo = new FormattingInfo(
                        formattingInfo.isLeftAligned(), formattingInfo.getMinLength(), formattingInfo.getMaxLength(), false, formattingInfo.isZeroPad()
                     );
                     continue;
                  default:
                     if (c >= '0' && c <= '9') {
                        formattingInfo = new FormattingInfo(
                           formattingInfo.isLeftAligned(), formattingInfo.getMinLength(), c - '0', formattingInfo.isLeftTruncate(), formattingInfo.isZeroPad()
                        );
                        state = PatternParser.ParserState.MAX_STATE;
                        continue;
                     }

                     LOGGER.error("Error occurred in position " + i + ".\n Was expecting digit, instead got char \"" + c + "\".");
                     state = PatternParser.ParserState.LITERAL_STATE;
                     continue;
               }
            case MAX_STATE:
               currentLiteral.append(c);
               if (c >= '0' && c <= '9') {
                  formattingInfo = new FormattingInfo(
                     formattingInfo.isLeftAligned(),
                     formattingInfo.getMinLength(),
                     formattingInfo.getMaxLength() * 10 + c - 48,
                     formattingInfo.isLeftTruncate(),
                     formattingInfo.isZeroPad()
                  );
               } else {
                  i = this.finalizeConverter(
                     c,
                     pattern,
                     i,
                     currentLiteral,
                     formattingInfo,
                     this.converterRules,
                     patternConverters,
                     formattingInfos,
                     disableAnsi,
                     noConsoleNoAnsi,
                     convertBackslashes
                  );
                  state = PatternParser.ParserState.LITERAL_STATE;
                  formattingInfo = FormattingInfo.getDefault();
                  currentLiteral.setLength(0);
               }
         }
      }

      if (currentLiteral.length() != 0) {
         patternConverters.add(this.literalPattern(currentLiteral.toString(), convertBackslashes));
         formattingInfos.add(FormattingInfo.getDefault());
      }
   }

   private PatternConverter createConverter(
      final String converterId,
      final StringBuilder currentLiteral,
      final Map<String, Class<? extends PatternConverter>> rules,
      final List<String> options,
      final boolean disableAnsi,
      final boolean noConsoleNoAnsi
   ) {
      String converterName = converterId;
      Class<? extends PatternConverter> converterClass = null;
      if (rules == null) {
         LOGGER.error("Null rules for [" + converterId + "]");
         return null;
      }

      for (int i = converterId.length(); i > 0 && converterClass == null; i--) {
         converterName = converterName.substring(0, i);
         converterClass = rules.get(converterName);
      }

      if (converterClass == null) {
         LOGGER.error("Unrecognized format specifier [" + converterId + "]");
         return null;
      }

      if (AnsiConverter.class.isAssignableFrom(converterClass)) {
         options.add("disableAnsi=" + disableAnsi);
         options.add("noConsoleNoAnsi=" + noConsoleNoAnsi);
      }

      Method[] methods = converterClass.getDeclaredMethods();
      Method newInstanceMethod = null;

      for (Method method : methods) {
         if (Modifier.isStatic(method.getModifiers())
            && method.getDeclaringClass().equals(converterClass)
            && method.getName().equals("newInstance")
            && areValidNewInstanceParameters(method.getParameterTypes())) {
            if (newInstanceMethod == null) {
               newInstanceMethod = method;
            } else if (method.getReturnType().equals(newInstanceMethod.getReturnType())) {
               LOGGER.error("Class " + converterClass + " cannot contain multiple static newInstance methods");
               return null;
            }
         }
      }

      if (newInstanceMethod == null) {
         LOGGER.error("Class " + converterClass + " does not contain a static newInstance method");
         return null;
      }

      Class<?>[] parmTypes = newInstanceMethod.getParameterTypes();
      Object[] parms = parmTypes.length > 0 ? new Object[parmTypes.length] : null;
      if (parms != null) {
         int i = 0;
         boolean errors = false;

         for (Class<?> clazz : parmTypes) {
            if (clazz.isArray() && clazz.getName().equals("[Ljava.lang.String;")) {
               String[] optionsArray = options.toArray(Strings.EMPTY_ARRAY);
               parms[i] = optionsArray;
            } else if (clazz.isAssignableFrom(Configuration.class)) {
               parms[i] = this.config;
            } else {
               LOGGER.error("Unknown parameter type " + clazz.getName() + " for static newInstance method of " + converterClass.getName());
               errors = true;
            }

            i++;
         }

         if (errors) {
            return null;
         }
      }

      try {
         Object newObj = newInstanceMethod.invoke(null, parms);
         if (newObj instanceof PatternConverter) {
            currentLiteral.delete(0, currentLiteral.length() - (converterId.length() - converterName.length()));
            return (PatternConverter)newObj;
         }

         LOGGER.warn("Class {} does not extend PatternConverter.", converterClass.getName());
      } catch (Exception ex) {
         LOGGER.error("Error creating converter for " + converterId, ex);
      }

      return null;
   }

   private static boolean areValidNewInstanceParameters(final Class<?>[] parameterTypes) {
      for (Class<?> clazz : parameterTypes) {
         if (!clazz.isAssignableFrom(Configuration.class) && (!clazz.isArray() || !"[Ljava.lang.String;".equals(clazz.getName()))) {
            return false;
         }
      }

      return true;
   }

   private int finalizeConverter(
      final char c,
      final String pattern,
      final int start,
      final StringBuilder currentLiteral,
      final FormattingInfo formattingInfo,
      final Map<String, Class<? extends PatternConverter>> rules,
      final List<PatternConverter> patternConverters,
      final List<FormattingInfo> formattingInfos,
      final boolean disableAnsi,
      final boolean noConsoleNoAnsi,
      final boolean convertBackslashes
   ) {
      int i = start;
      StringBuilder convBuf = new StringBuilder();
      i = extractConverter(c, pattern, i, convBuf, currentLiteral);
      String converterId = convBuf.toString();
      List<String> options = new ArrayList<>();
      i = extractOptions(pattern, i, options);
      PatternConverter pc = this.createConverter(converterId, currentLiteral, rules, options, disableAnsi, noConsoleNoAnsi);
      if (pc == null) {
         StringBuilder msg;
         if (Strings.isEmpty(converterId)) {
            msg = new StringBuilder("Empty conversion specifier starting at position ");
         } else {
            msg = new StringBuilder("Unrecognized conversion specifier [");
            msg.append(converterId);
            msg.append("] starting at position ");
         }

         msg.append(i);
         msg.append(" in conversion pattern.");
         LOGGER.error(msg.toString());
         patternConverters.add(this.literalPattern(currentLiteral.toString(), convertBackslashes));
         formattingInfos.add(FormattingInfo.getDefault());
      } else {
         patternConverters.add(pc);
         formattingInfos.add(formattingInfo);
         if (currentLiteral.length() > 0) {
            patternConverters.add(this.literalPattern(currentLiteral.toString(), convertBackslashes));
            formattingInfos.add(FormattingInfo.getDefault());
         }
      }

      currentLiteral.setLength(0);
      return i;
   }

   private LogEventPatternConverter literalPattern(final String literal, final boolean convertBackslashes) {
      return this.config != null && LiteralPatternConverter.containsSubstitutionSequence(literal)
         ? new LiteralPatternConverter(this.config, literal, convertBackslashes)
         : SimpleLiteralPatternConverter.of(literal, convertBackslashes);
   }

   private enum ParserState {
      LITERAL_STATE,
      CONVERTER_STATE,
      DOT_STATE,
      MIN_STATE,
      MAX_STATE;
   }
}
