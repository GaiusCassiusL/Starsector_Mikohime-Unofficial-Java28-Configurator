package org.apache.logging.log4j.core.lookup;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationAware;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

public class StrSubstitutor implements ConfigurationAware {
   public static final char DEFAULT_ESCAPE = '$';
   public static final StrMatcher DEFAULT_PREFIX = StrMatcher.stringMatcher("${");
   public static final StrMatcher DEFAULT_SUFFIX = StrMatcher.stringMatcher("}");
   public static final String DEFAULT_VALUE_DELIMITER_STRING = ":-";
   public static final StrMatcher DEFAULT_VALUE_DELIMITER = StrMatcher.stringMatcher(":-");
   public static final String ESCAPE_DELIMITER_STRING = ":\\-";
   public static final StrMatcher DEFAULT_VALUE_ESCAPE_DELIMITER = StrMatcher.stringMatcher(":\\-");
   private static final int BUF_SIZE = 256;
   private char escapeChar;
   private StrMatcher prefixMatcher;
   private StrMatcher suffixMatcher;
   private String valueDelimiterString;
   private StrMatcher valueDelimiterMatcher;
   private StrMatcher valueEscapeDelimiterMatcher;
   private StrLookup variableResolver;
   private boolean enableSubstitutionInVariables = true;
   private Configuration configuration;
   private boolean recursiveEvaluationAllowed;

   public StrSubstitutor() {
      this(null, DEFAULT_PREFIX, DEFAULT_SUFFIX, '$');
   }

   public StrSubstitutor(final Map<String, String> valueMap) {
      this(new PropertiesLookup(valueMap), DEFAULT_PREFIX, DEFAULT_SUFFIX, '$');
   }

   public StrSubstitutor(final Map<String, String> valueMap, final String prefix, final String suffix) {
      this(new PropertiesLookup(valueMap), prefix, suffix, '$');
   }

   public StrSubstitutor(final Map<String, String> valueMap, final String prefix, final String suffix, final char escape) {
      this(new PropertiesLookup(valueMap), prefix, suffix, escape);
   }

   public StrSubstitutor(final Map<String, String> valueMap, final String prefix, final String suffix, final char escape, final String valueDelimiter) {
      this(new PropertiesLookup(valueMap), prefix, suffix, escape, valueDelimiter);
   }

   public StrSubstitutor(final Properties properties) {
      this(toTypeSafeMap(properties));
   }

   public StrSubstitutor(final StrLookup variableResolver) {
      this(variableResolver, DEFAULT_PREFIX, DEFAULT_SUFFIX, '$');
   }

   public StrSubstitutor(final StrLookup variableResolver, final String prefix, final String suffix, final char escape) {
      this.setVariableResolver(variableResolver);
      this.setVariablePrefix(prefix);
      this.setVariableSuffix(suffix);
      this.setEscapeChar(escape);
   }

   public StrSubstitutor(final StrLookup variableResolver, final String prefix, final String suffix, final char escape, final String valueDelimiter) {
      this.setVariableResolver(variableResolver);
      this.setVariablePrefix(prefix);
      this.setVariableSuffix(suffix);
      this.setEscapeChar(escape);
      this.setValueDelimiter(valueDelimiter);
   }

   public StrSubstitutor(final StrLookup variableResolver, final StrMatcher prefixMatcher, final StrMatcher suffixMatcher, final char escape) {
      this(variableResolver, prefixMatcher, suffixMatcher, escape, DEFAULT_VALUE_DELIMITER, DEFAULT_VALUE_ESCAPE_DELIMITER);
      this.valueDelimiterString = ":-";
   }

   public StrSubstitutor(
      final StrLookup variableResolver,
      final StrMatcher prefixMatcher,
      final StrMatcher suffixMatcher,
      final char escape,
      final StrMatcher valueDelimiterMatcher
   ) {
      this.setVariableResolver(variableResolver);
      this.setVariablePrefixMatcher(prefixMatcher);
      this.setVariableSuffixMatcher(suffixMatcher);
      this.setEscapeChar(escape);
      this.setValueDelimiterMatcher(valueDelimiterMatcher);
   }

   public StrSubstitutor(
      final StrLookup variableResolver,
      final StrMatcher prefixMatcher,
      final StrMatcher suffixMatcher,
      final char escape,
      final StrMatcher valueDelimiterMatcher,
      final StrMatcher valueEscapeMatcher
   ) {
      this.setVariableResolver(variableResolver);
      this.setVariablePrefixMatcher(prefixMatcher);
      this.setVariableSuffixMatcher(suffixMatcher);
      this.setEscapeChar(escape);
      this.setValueDelimiterMatcher(valueDelimiterMatcher);
      this.valueEscapeDelimiterMatcher = valueEscapeMatcher;
   }

   StrSubstitutor(final StrSubstitutor other) {
      Objects.requireNonNull(other, "other");
      this.setVariableResolver(other.getVariableResolver());
      this.setVariablePrefixMatcher(other.getVariablePrefixMatcher());
      this.setVariableSuffixMatcher(other.getVariableSuffixMatcher());
      this.setEscapeChar(other.getEscapeChar());
      this.setValueDelimiterMatcher(other.valueDelimiterMatcher);
      this.valueEscapeDelimiterMatcher = other.valueEscapeDelimiterMatcher;
      this.configuration = other.configuration;
      this.recursiveEvaluationAllowed = other.isRecursiveEvaluationAllowed();
      this.enableSubstitutionInVariables = other.isEnableSubstitutionInVariables();
      this.valueDelimiterString = other.valueDelimiterString;
   }

   public static String replace(final Object source, final Map<String, String> valueMap) {
      return new StrSubstitutor(valueMap).replace(source);
   }

   public static String replace(final Object source, final Map<String, String> valueMap, final String prefix, final String suffix) {
      return new StrSubstitutor(valueMap, prefix, suffix).replace(source);
   }

   public static String replace(final Object source, final Properties valueProperties) {
      if (valueProperties == null) {
         return source.toString();
      }

      Map<String, String> valueMap = new HashMap<>();
      Enumeration<?> propNames = valueProperties.propertyNames();

      while (propNames.hasMoreElements()) {
         String propName = (String)propNames.nextElement();
         String propValue = valueProperties.getProperty(propName);
         valueMap.put(propName, propValue);
      }

      return replace(source, valueMap);
   }

   private static Map<String, String> toTypeSafeMap(final Properties properties) {
      Map<String, String> map = new HashMap<>(properties.size());

      for (String name : properties.stringPropertyNames()) {
         map.put(name, properties.getProperty(name));
      }

      return map;
   }

   private static String handleFailedReplacement(final String input, final Throwable throwable) {
      StatusLogger.getLogger().error("Replacement failed on {}", input, throwable);
      return input;
   }

   public String replace(final String source) {
      return this.replace(null, source);
   }

   public String replace(final LogEvent event, final String source) {
      if (source == null) {
         return null;
      }

      StringBuilder buf = new StringBuilder(source);

      try {
         if (!this.substitute(event, buf, 0, source.length())) {
            return source;
         }
      } catch (Throwable t) {
         return handleFailedReplacement(source, t);
      }

      return buf.toString();
   }

   public String replace(final String source, final int offset, final int length) {
      return this.replace(null, source, offset, length);
   }

   public String replace(final LogEvent event, final String source, final int offset, final int length) {
      if (source == null) {
         return null;
      }

      StringBuilder buf = new StringBuilder(length).append(source, offset, length);

      try {
         if (!this.substitute(event, buf, 0, length)) {
            return source.substring(offset, offset + length);
         }
      } catch (Throwable t) {
         return handleFailedReplacement(source, t);
      }

      return buf.toString();
   }

   public String replace(final char[] source) {
      return this.replace(null, source);
   }

   public String replace(final LogEvent event, final char[] source) {
      if (source == null) {
         return null;
      }

      StringBuilder buf = new StringBuilder(source.length).append(source);

      try {
         this.substitute(event, buf, 0, source.length);
      } catch (Throwable t) {
         return handleFailedReplacement(new String(source), t);
      }

      return buf.toString();
   }

   public String replace(final char[] source, final int offset, final int length) {
      return this.replace(null, source, offset, length);
   }

   public String replace(final LogEvent event, final char[] source, final int offset, final int length) {
      if (source == null) {
         return null;
      }

      StringBuilder buf = new StringBuilder(length).append(source, offset, length);

      try {
         this.substitute(event, buf, 0, length);
      } catch (Throwable t) {
         return handleFailedReplacement(new String(source, offset, length), t);
      }

      return buf.toString();
   }

   public String replace(final StringBuffer source) {
      return this.replace(null, source);
   }

   public String replace(final LogEvent event, final StringBuffer source) {
      if (source == null) {
         return null;
      }

      StringBuilder buf = new StringBuilder(source.length()).append(source);

      try {
         this.substitute(event, buf, 0, buf.length());
      } catch (Throwable t) {
         return handleFailedReplacement(source.toString(), t);
      }

      return buf.toString();
   }

   public String replace(final StringBuffer source, final int offset, final int length) {
      return this.replace(null, source, offset, length);
   }

   public String replace(final LogEvent event, final StringBuffer source, final int offset, final int length) {
      if (source == null) {
         return null;
      }

      StringBuilder buf = new StringBuilder(length).append(source, offset, length);

      try {
         this.substitute(event, buf, 0, length);
      } catch (Throwable t) {
         return handleFailedReplacement(source.substring(offset, offset + length), t);
      }

      return buf.toString();
   }

   public String replace(final StringBuilder source) {
      return this.replace(null, source);
   }

   public String replace(final LogEvent event, final StringBuilder source) {
      if (source == null) {
         return null;
      }

      StringBuilder buf = new StringBuilder(source.length()).append(source);

      try {
         this.substitute(event, buf, 0, buf.length());
      } catch (Throwable t) {
         return handleFailedReplacement(source.toString(), t);
      }

      return buf.toString();
   }

   public String replace(final StringBuilder source, final int offset, final int length) {
      return this.replace(null, source, offset, length);
   }

   public String replace(final LogEvent event, final StringBuilder source, final int offset, final int length) {
      if (source == null) {
         return null;
      }

      StringBuilder buf = new StringBuilder(length).append(source, offset, length);

      try {
         this.substitute(event, buf, 0, length);
      } catch (Throwable t) {
         return handleFailedReplacement(source.substring(offset, offset + length), t);
      }

      return buf.toString();
   }

   public String replace(final Object source) {
      return this.replace(null, source);
   }

   public String replace(final LogEvent event, final Object source) {
      if (source == null) {
         return null;
      }

      String stringValue = String.valueOf(source);
      StringBuilder buf = new StringBuilder(stringValue.length()).append(stringValue);

      try {
         this.substitute(event, buf, 0, buf.length());
      } catch (Throwable t) {
         return handleFailedReplacement(stringValue, t);
      }

      return buf.toString();
   }

   public boolean replaceIn(final StringBuffer source) {
      return source == null ? false : this.replaceIn(source, 0, source.length());
   }

   public boolean replaceIn(final StringBuffer source, final int offset, final int length) {
      return this.replaceIn(null, source, offset, length);
   }

   public boolean replaceIn(final LogEvent event, final StringBuffer source, final int offset, final int length) {
      if (source == null) {
         return false;
      }

      StringBuilder buf = new StringBuilder(length).append(source, offset, length);

      try {
         if (!this.substitute(event, buf, 0, length)) {
            return false;
         }
      } catch (Throwable t) {
         StatusLogger.getLogger().error("Replacement failed on {}", source, t);
         return false;
      }

      source.replace(offset, offset + length, buf.toString());
      return true;
   }

   public boolean replaceIn(final StringBuilder source) {
      return this.replaceIn(null, source);
   }

   public boolean replaceIn(final LogEvent event, final StringBuilder source) {
      return source == null ? false : this.substitute(event, source, 0, source.length());
   }

   public boolean replaceIn(final StringBuilder source, final int offset, final int length) {
      return this.replaceIn(null, source, offset, length);
   }

   public boolean replaceIn(final LogEvent event, final StringBuilder source, final int offset, final int length) {
      return source == null ? false : this.substitute(event, source, offset, length);
   }

   protected boolean substitute(final LogEvent event, final StringBuilder buf, final int offset, final int length) {
      return this.substitute(event, buf, offset, length, null) > 0;
   }

   private int substitute(final LogEvent event, final StringBuilder buf, final int offset, final int length, List<String> priorVariables) {
      StrMatcher prefixMatcher = this.getVariablePrefixMatcher();
      StrMatcher suffixMatcher = this.getVariableSuffixMatcher();
      char escape = this.getEscapeChar();
      StrMatcher valueDelimiterMatcher = this.getValueDelimiterMatcher();
      boolean substitutionInVariablesEnabled = this.isEnableSubstitutionInVariables();
      boolean top = priorVariables == null;
      boolean altered = false;
      int lengthChange = 0;
      char[] chars = this.getChars(buf);
      int bufEnd = offset + length;
      int pos = offset;

      while (pos < bufEnd) {
         int startMatchLen = prefixMatcher.isMatch(chars, pos, offset, bufEnd);
         if (startMatchLen == 0) {
            pos++;
         } else if (pos > offset && chars[pos - 1] == escape) {
            buf.deleteCharAt(pos - 1);
            chars = this.getChars(buf);
            lengthChange--;
            altered = true;
            bufEnd--;
         } else {
            int startPos = pos;
            pos += startMatchLen;
            int endMatchLen = 0;
            int nestedVarCount = 0;

            while (pos < bufEnd) {
               if (substitutionInVariablesEnabled && (endMatchLen = prefixMatcher.isMatch(chars, pos, offset, bufEnd)) != 0) {
                  nestedVarCount++;
                  pos += endMatchLen;
               } else {
                  endMatchLen = suffixMatcher.isMatch(chars, pos, offset, bufEnd);
                  if (endMatchLen == 0) {
                     pos++;
                  } else {
                     if (nestedVarCount == 0) {
                        String varNameExpr = new String(chars, startPos + startMatchLen, pos - startPos - startMatchLen);
                        if (substitutionInVariablesEnabled) {
                           if (priorVariables == null) {
                              priorVariables = new ArrayList<>();
                           }

                           StringBuilder bufName = new StringBuilder(varNameExpr);
                           this.substitute(event, bufName, 0, bufName.length(), priorVariables);
                           varNameExpr = bufName.toString();
                        }

                        pos += endMatchLen;
                        int endPos = pos;
                        String varName = varNameExpr;
                        String varDefaultValue = null;
                        if (valueDelimiterMatcher != null) {
                           char[] varNameExprChars = varNameExpr.toCharArray();
                           int valueDelimiterMatchLen = 0;

                           label116:
                           for (int i = 0;
                              i < varNameExprChars.length
                                 && (substitutionInVariablesEnabled || prefixMatcher.isMatch(varNameExprChars, i, i, varNameExprChars.length) == 0);
                              i++
                           ) {
                              if (this.valueEscapeDelimiterMatcher != null) {
                                 int matchLen = this.valueEscapeDelimiterMatcher.isMatch(varNameExprChars, i);
                                 if (matchLen != 0) {
                                    String varNamePrefix = varNameExpr.substring(0, i) + ":";
                                    varName = varNamePrefix + varNameExpr.substring(i + matchLen - 1);

                                    for (int j = i + matchLen; j < varNameExprChars.length; j++) {
                                       if ((valueDelimiterMatchLen = valueDelimiterMatcher.isMatch(varNameExprChars, j)) != 0) {
                                          varName = varNamePrefix + varNameExpr.substring(i + matchLen, j);
                                          varDefaultValue = varNameExpr.substring(j + valueDelimiterMatchLen);
                                          break label116;
                                       }
                                    }
                                    break;
                                 }

                                 if ((valueDelimiterMatchLen = valueDelimiterMatcher.isMatch(varNameExprChars, i)) != 0) {
                                    varName = varNameExpr.substring(0, i);
                                    varDefaultValue = varNameExpr.substring(i + valueDelimiterMatchLen);
                                    break;
                                 }
                              } else if ((valueDelimiterMatchLen = valueDelimiterMatcher.isMatch(varNameExprChars, i)) != 0) {
                                 varName = varNameExpr.substring(0, i);
                                 varDefaultValue = varNameExpr.substring(i + valueDelimiterMatchLen);
                                 break;
                              }
                           }
                        }

                        if (priorVariables == null) {
                           priorVariables = new ArrayList<>();
                           priorVariables.add(new String(chars, offset, length + lengthChange));
                        }

                        boolean isCyclic = this.isCyclicSubstitution(varName, priorVariables);
                        String varValue = isCyclic ? null : this.resolveVariable(event, varName, buf, startPos, endPos);
                        if (varValue == null) {
                           varValue = varDefaultValue;
                        }

                        if (varValue != null) {
                           int varLen = varValue.length();
                           buf.replace(startPos, endPos, varValue);
                           altered = true;
                           int change = this.isRecursiveEvaluationAllowed() ? this.substitute(event, buf, startPos, varLen, priorVariables) : 0;
                           change += varLen - (endPos - startPos);
                           pos += change;
                           bufEnd += change;
                           lengthChange += change;
                           chars = this.getChars(buf);
                        }

                        if (!isCyclic) {
                           priorVariables.remove(priorVariables.size() - 1);
                        }
                        break;
                     }

                     nestedVarCount--;
                     pos += endMatchLen;
                  }
               }
            }
         }
      }

      if (top) {
         return altered ? 1 : 0;
      } else {
         return lengthChange;
      }
   }

   private boolean isCyclicSubstitution(final String varName, final List<String> priorVariables) {
      if (!priorVariables.contains(varName)) {
         priorVariables.add(varName);
         return false;
      } else {
         StringBuilder buf = new StringBuilder(256);
         buf.append("Infinite loop in property interpolation of ");
         this.appendWithSeparators(buf, priorVariables, "->");
         StatusLogger.getLogger().warn(buf);
         return true;
      }
   }

   protected String resolveVariable(final LogEvent event, final String variableName, final StringBuilder buf, final int startPos, final int endPos) {
      StrLookup resolver = this.getVariableResolver();
      if (resolver == null) {
         return null;
      }

      try {
         return resolver.lookup(event, variableName);
      } catch (Throwable t) {
         StatusLogger.getLogger().error("Resolver failed to lookup {}", variableName, t);
         return null;
      }
   }

   public char getEscapeChar() {
      return this.escapeChar;
   }

   public void setEscapeChar(final char escapeCharacter) {
      this.escapeChar = escapeCharacter;
   }

   public StrMatcher getVariablePrefixMatcher() {
      return this.prefixMatcher;
   }

   public StrSubstitutor setVariablePrefixMatcher(final StrMatcher prefixMatcher) {
      if (prefixMatcher == null) {
         throw new IllegalArgumentException("Parameter prefixMatcher must not be null!");
      }

      this.prefixMatcher = prefixMatcher;
      return this;
   }

   public StrSubstitutor setVariablePrefix(final char prefix) {
      return this.setVariablePrefixMatcher(StrMatcher.charMatcher(prefix));
   }

   public StrSubstitutor setVariablePrefix(final String prefix) {
      if (prefix == null) {
         throw new IllegalArgumentException("Variable prefix must not be null!");
      } else {
         return this.setVariablePrefixMatcher(StrMatcher.stringMatcher(prefix));
      }
   }

   public StrMatcher getVariableSuffixMatcher() {
      return this.suffixMatcher;
   }

   public StrSubstitutor setVariableSuffixMatcher(final StrMatcher suffixMatcher) {
      if (suffixMatcher == null) {
         throw new IllegalArgumentException("Parameter suffixMatcher must not be null!");
      }

      this.suffixMatcher = suffixMatcher;
      return this;
   }

   public StrSubstitutor setVariableSuffix(final char suffix) {
      return this.setVariableSuffixMatcher(StrMatcher.charMatcher(suffix));
   }

   public StrSubstitutor setVariableSuffix(final String suffix) {
      if (suffix == null) {
         throw new IllegalArgumentException("Variable suffix must not be null!");
      } else {
         return this.setVariableSuffixMatcher(StrMatcher.stringMatcher(suffix));
      }
   }

   public StrMatcher getValueDelimiterMatcher() {
      return this.valueDelimiterMatcher;
   }

   public StrSubstitutor setValueDelimiterMatcher(final StrMatcher valueDelimiterMatcher) {
      this.valueDelimiterMatcher = valueDelimiterMatcher;
      return this;
   }

   public StrSubstitutor setValueDelimiter(final char valueDelimiter) {
      return this.setValueDelimiterMatcher(StrMatcher.charMatcher(valueDelimiter));
   }

   public StrSubstitutor setValueDelimiter(final String valueDelimiter) {
      if (Strings.isEmpty(valueDelimiter)) {
         this.setValueDelimiterMatcher(null);
         return this;
      } else {
         String escapeValue = valueDelimiter.substring(0, valueDelimiter.length() - 1) + "\\" + valueDelimiter.substring(valueDelimiter.length() - 1);
         this.valueEscapeDelimiterMatcher = StrMatcher.stringMatcher(escapeValue);
         return this.setValueDelimiterMatcher(StrMatcher.stringMatcher(valueDelimiter));
      }
   }

   public StrLookup getVariableResolver() {
      return this.variableResolver;
   }

   public void setVariableResolver(final StrLookup variableResolver) {
      if (variableResolver instanceof ConfigurationAware && this.configuration != null) {
         ((ConfigurationAware)variableResolver).setConfiguration(this.configuration);
      }

      this.variableResolver = variableResolver;
   }

   public boolean isEnableSubstitutionInVariables() {
      return this.enableSubstitutionInVariables;
   }

   public void setEnableSubstitutionInVariables(final boolean enableSubstitutionInVariables) {
      this.enableSubstitutionInVariables = enableSubstitutionInVariables;
   }

   boolean isRecursiveEvaluationAllowed() {
      return this.recursiveEvaluationAllowed;
   }

   void setRecursiveEvaluationAllowed(final boolean recursiveEvaluationAllowed) {
      this.recursiveEvaluationAllowed = recursiveEvaluationAllowed;
   }

   private char[] getChars(final StringBuilder sb) {
      char[] chars = new char[sb.length()];
      sb.getChars(0, sb.length(), chars, 0);
      return chars;
   }

   public void appendWithSeparators(final StringBuilder sb, final Iterable<?> iterable, String separator) {
      if (iterable != null) {
         separator = separator == null ? "" : separator;
         Iterator<?> it = iterable.iterator();

         while (it.hasNext()) {
            sb.append(it.next());
            if (it.hasNext()) {
               sb.append(separator);
            }
         }
      }
   }

   @Override
   public String toString() {
      return "StrSubstitutor(" + this.variableResolver.toString() + ")";
   }

   @Override
   public void setConfiguration(final Configuration configuration) {
      this.configuration = configuration;
      if (this.variableResolver instanceof ConfigurationAware) {
         ((ConfigurationAware)this.variableResolver).setConfiguration(this.configuration);
      }
   }
}
