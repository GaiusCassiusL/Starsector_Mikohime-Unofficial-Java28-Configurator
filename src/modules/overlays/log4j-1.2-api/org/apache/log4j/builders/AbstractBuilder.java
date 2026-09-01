package org.apache.log4j.builders;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.bridge.FilterWrapper;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;
import org.w3c.dom.Element;

public abstract class AbstractBuilder<T> implements Builder<T> {
   private static final Logger LOGGER = StatusLogger.getLogger();
   protected static final String FILE_PARAM = "File";
   protected static final String APPEND_PARAM = "Append";
   protected static final String BUFFERED_IO_PARAM = "BufferedIO";
   protected static final String BUFFER_SIZE_PARAM = "BufferSize";
   protected static final String IMMEDIATE_FLUSH_PARAM = "ImmediateFlush";
   protected static final String MAX_SIZE_PARAM = "MaxFileSize";
   protected static final String MAX_BACKUP_INDEX = "MaxBackupIndex";
   protected static final String RELATIVE = "RELATIVE";
   protected static final String NULL = "NULL";
   private final String prefix;
   private final Properties properties;

   public AbstractBuilder() {
      this(null, new Properties());
   }

   public AbstractBuilder(final String prefix, final Properties props) {
      this.prefix = prefix != null ? prefix + "." : null;
      this.properties = (Properties)props.clone();
      Map<String, String> map = new HashMap<>();
      System.getProperties().forEach((k, v) -> map.put(k.toString(), v.toString()));
      props.forEach((k, v) -> map.put(k.toString(), v.toString()));
      props.forEach((k, v) -> map.put(this.toBeanKey(k.toString()), v.toString()));
      props.entrySet().forEach(e -> this.properties.put(this.toBeanKey(e.getKey().toString()), e.getValue()));
   }

   protected static Filter buildFilters(final String level, final org.apache.log4j.spi.Filter filter) {
      org.apache.log4j.spi.Filter head = null;
      if (level != null) {
         Filter thresholdFilter = ThresholdFilter.createFilter(OptionConverter.convertLevel(level, Level.TRACE), Result.NEUTRAL, Result.DENY);
         head = new FilterWrapper(thresholdFilter);
      }

      if (filter != null) {
         head = FilterAdapter.addFilter(head, filter);
      }

      return FilterAdapter.adapt(head);
   }

   private String capitalize(final String value) {
      if (!Strings.isEmpty(value) && !Character.isUpperCase(value.charAt(0))) {
         char[] chars = value.toCharArray();
         chars[0] = Character.toUpperCase(chars[0]);
         return new String(chars);
      } else {
         return value;
      }
   }

   public boolean getBooleanProperty(final String key, final boolean defaultValue) {
      return Boolean.parseBoolean(this.getProperty(key, Boolean.toString(defaultValue)));
   }

   public boolean getBooleanProperty(final String key) {
      return this.getBooleanProperty(key, false);
   }

   protected boolean getBooleanValueAttribute(final Element element) {
      return Boolean.parseBoolean(this.getValueAttribute(element));
   }

   public int getIntegerProperty(final String key, final int defaultValue) {
      String value = null;

      try {
         value = this.getProperty(key);
         if (value != null) {
            return Integer.parseInt(value);
         }
      } catch (Exception ex) {
         LOGGER.warn("Error converting value {} of {} to an integer: {}", value, key, ex.getMessage());
      }

      return defaultValue;
   }

   protected String getNameAttribute(final Element element) {
      return element.getAttribute("name");
   }

   protected String getNameAttributeKey(final Element element) {
      return this.toBeanKey(element.getAttribute("name"));
   }

   public Properties getProperties() {
      return this.properties;
   }

   public String getProperty(final String key) {
      return this.getProperty(key, null);
   }

   public String getProperty(final String key, final String defaultValue) {
      String value = this.properties.getProperty(this.prefix + this.toJavaKey(key));
      value = value != null ? value : this.properties.getProperty(this.prefix + this.toBeanKey(key), defaultValue);
      value = value != null ? this.substVars(value) : defaultValue;
      return value != null ? value.trim() : defaultValue;
   }

   protected String getValueAttribute(final Element element) {
      return this.getValueAttribute(element, null);
   }

   protected String getValueAttribute(final Element element, final String defaultValue) {
      String attribute = element.getAttribute("value");
      return this.substVars(attribute != null ? attribute.trim() : defaultValue);
   }

   protected String substVars(final String value) {
      return OptionConverter.substVars(value, this.properties);
   }

   String toBeanKey(final String value) {
      return this.capitalize(value);
   }

   String toJavaKey(final String value) {
      return this.uncapitalize(value);
   }

   private String uncapitalize(final String value) {
      if (!Strings.isEmpty(value) && !Character.isLowerCase(value.charAt(0))) {
         char[] chars = value.toCharArray();
         chars[0] = Character.toLowerCase(chars[0]);
         return new String(chars);
      } else {
         return value;
      }
   }

   protected void set(final String name, final Element element, final AtomicBoolean ref) {
      String value = this.getValueAttribute(element);
      if (value == null) {
         LOGGER.warn("No value for {} parameter, using default {}", name, ref);
      } else {
         ref.set(Boolean.parseBoolean(value));
      }
   }

   protected void set(final String name, final Element element, final AtomicInteger ref) {
      String value = this.getValueAttribute(element);
      if (value == null) {
         LOGGER.warn("No value for {} parameter, using default {}", name, ref);
      } else {
         try {
            ref.set(Integer.parseInt(value));
         } catch (NumberFormatException e) {
            LOGGER.warn("{} parsing {} parameter, using default {}: {}", e.getClass().getName(), name, ref, e.getMessage(), e);
         }
      }
   }

   protected void set(final String name, final Element element, final AtomicReference<String> ref) {
      String value = this.getValueAttribute(element);
      if (value == null) {
         LOGGER.warn("No value for {} parameter, using default {}", name, ref);
      } else {
         ref.set(value);
      }
   }
}
