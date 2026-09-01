package org.apache.logging.log4j.core.util;

import java.io.InterruptedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.apache.logging.log4j.util.Strings;

public final class OptionConverter {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final String DELIM_START = "${";
   private static final char DELIM_STOP = '}';
   private static final int DELIM_START_LEN = 2;
   private static final int DELIM_STOP_LEN = 1;
   private static final int ONE_K = 1024;

   private OptionConverter() {
   }

   public static String[] concatenateArrays(final String[] l, final String[] r) {
      int len = l.length + r.length;
      String[] a = new String[len];
      System.arraycopy(l, 0, a, 0, l.length);
      System.arraycopy(r, 0, a, l.length, r.length);
      return a;
   }

   public static String convertSpecialChars(final String s) {
      int len = s.length();
      StringBuilder sbuf = new StringBuilder(len);
      int i = 0;

      while (i < len) {
         char c = s.charAt(i++);
         if (c == '\\') {
            c = s.charAt(i++);
            switch (c) {
               case '"':
                  c = '"';
                  break;
               case '\'':
                  c = '\'';
                  break;
               case '\\':
                  c = '\\';
                  break;
               case 'b':
                  c = '\b';
                  break;
               case 'f':
                  c = '\f';
                  break;
               case 'n':
                  c = '\n';
                  break;
               case 'r':
                  c = '\r';
                  break;
               case 't':
                  c = '\t';
            }
         }

         sbuf.append(c);
      }

      return sbuf.toString();
   }

   public static Object instantiateByKey(final Properties props, final String key, final Class<?> superClass, final Object defaultValue) {
      String className = findAndSubst(key, props);
      if (className == null) {
         LOGGER.error("Could not find value for key {}", key);
         return defaultValue;
      } else {
         return instantiateByClassName(className.trim(), superClass, defaultValue);
      }
   }

   public static boolean toBoolean(final String value, final boolean defaultValue) {
      if (value == null) {
         return defaultValue;
      } else {
         String trimmedVal = value.trim();
         if ("true".equalsIgnoreCase(trimmedVal)) {
            return true;
         } else {
            return "false".equalsIgnoreCase(trimmedVal) ? false : defaultValue;
         }
      }
   }

   public static int toInt(final String value, final int defaultValue) {
      if (value != null) {
         String s = value.trim();

         try {
            return Integer.parseInt(s);
         } catch (NumberFormatException e) {
            LOGGER.error("[{}] is not in proper int form.", s, e);
         }
      }

      return defaultValue;
   }

   public static Level toLevel(String value, Level defaultValue) {
      if (value == null) {
         return defaultValue;
      }

      value = value.trim();
      int hashIndex = value.indexOf(35);
      if (hashIndex == -1) {
         return "NULL".equalsIgnoreCase(value) ? null : Level.toLevel(value, defaultValue);
      }

      Level result = defaultValue;
      String clazz = value.substring(hashIndex + 1);
      String levelName = value.substring(0, hashIndex);
      if ("NULL".equalsIgnoreCase(levelName)) {
         return null;
      }

      LOGGER.debug("toLevel:class=[" + clazz + "]:pri=[" + levelName + "]");

      try {
         Class<?> customLevel = Loader.loadClass(clazz);
         Class<?>[] paramTypes = new Class[]{String.class, Level.class};
         Method toLevelMethod = customLevel.getMethod("toLevel", paramTypes);
         Object[] params = new Object[]{levelName, defaultValue};
         Object o = toLevelMethod.invoke(null, params);
         result = (Level)o;
      } catch (ClassNotFoundException e) {
         LOGGER.warn("custom level class [" + clazz + "] not found.");
      } catch (NoSuchMethodException e) {
         LOGGER.warn("custom level class [" + clazz + "] does not have a class function toLevel(String, Level)", e);
      } catch (InvocationTargetException e) {
         if (e.getTargetException() instanceof InterruptedException || e.getTargetException() instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.warn("custom level class [" + clazz + "] could not be instantiated", e);
      } catch (ClassCastException e) {
         LOGGER.warn("class [" + clazz + "] is not a subclass of org.apache.log4j.Level", e);
      } catch (IllegalAccessException e) {
         LOGGER.warn("class [" + clazz + "] cannot be instantiated due to access restrictions", e);
      } catch (RuntimeException e) {
         LOGGER.warn("class [" + clazz + "], level [" + levelName + "] conversion failed.", e);
      }

      return result;
   }

   public static long toFileSize(final String value, final long defaultValue) {
      if (value == null) {
         return defaultValue;
      }

      String str = value.trim().toUpperCase(Locale.ENGLISH);
      long multiplier = 1L;
      int index;
      if ((index = str.indexOf("KB")) != -1) {
         multiplier = 1024L;
         str = str.substring(0, index);
      } else if ((index = str.indexOf("MB")) != -1) {
         multiplier = 1048576L;
         str = str.substring(0, index);
      } else if ((index = str.indexOf("GB")) != -1) {
         multiplier = 1073741824L;
         str = str.substring(0, index);
      }

      try {
         return Long.parseLong(str) * multiplier;
      } catch (NumberFormatException e) {
         LOGGER.error("[{}] is not in proper int form.", str);
         LOGGER.error("[{}] not in expected format.", value, e);
         return defaultValue;
      }
   }

   public static String findAndSubst(final String key, final Properties props) {
      String value = props.getProperty(key);
      if (value == null) {
         return null;
      }

      try {
         return substVars(value, props);
      } catch (IllegalArgumentException e) {
         LOGGER.error("Bad option value [{}].", value, e);
         return value;
      }
   }

   public static Object instantiateByClassName(final String className, final Class<?> superClass, final Object defaultValue) {
      if (className != null) {
         try {
            Class<?> classObj = Loader.loadClass(className);
            if (!superClass.isAssignableFrom(classObj)) {
               LOGGER.error("A \"{}\" object is not assignable to a \"{}\" variable.", className, superClass.getName());
               LOGGER.error(
                  "The class \"{}\" was loaded by [{}] whereas object of type [{}] was loaded by [{}].",
                  superClass.getName(),
                  superClass.getClassLoader(),
                  classObj.getTypeName(),
                  classObj.getName()
               );
               return defaultValue;
            }

            return classObj.newInstance();
         } catch (Exception e) {
            LOGGER.error("Could not instantiate class [{}].", className, e);
         }
      }

      return defaultValue;
   }

   public static String substVars(final String val, final Properties props) throws IllegalArgumentException {
      return substVars(val, props, new ArrayList<>());
   }

   private static String substVars(final String val, final Properties props, final List<String> keys) throws IllegalArgumentException {
      StringBuilder sbuf = new StringBuilder();
      int i = 0;

      while (true) {
         int j = val.indexOf("${", i);
         if (j == -1) {
            if (i == 0) {
               return val;
            }

            sbuf.append(val.substring(i, val.length()));
            return sbuf.toString();
         }

         sbuf.append(val.substring(i, j));
         int k = val.indexOf(125, j);
         if (k == -1) {
            throw new IllegalArgumentException(Strings.dquote(val) + " has no closing brace. Opening brace at position " + j + ".");
         }

         j += 2;
         String key = val.substring(j, k);
         String replacement = PropertiesUtil.getProperties().getStringProperty(key, null);
         if (replacement == null && props != null) {
            replacement = props.getProperty(key);
         }

         if (replacement != null) {
            if (!keys.contains(key)) {
               List<String> usedKeys = new ArrayList<>(keys);
               usedKeys.add(key);
               String recursiveReplacement = substVars(replacement, props, usedKeys);
               sbuf.append(recursiveReplacement);
            } else {
               sbuf.append(replacement);
            }
         }

         i = k + 1;
      }
   }
}
