package org.apache.logging.log4j.core.filter;

import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.ContextDataInjector;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.impl.ContextDataFactory;
import org.apache.logging.log4j.core.impl.ContextDataInjectorFactory;
import org.apache.logging.log4j.core.util.KeyValuePair;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.PerformanceSensitive;
import org.apache.logging.log4j.util.ReadOnlyStringMap;
import org.apache.logging.log4j.util.StringMap;

@Configurable(elementType = "filter", printObject = true)
@Plugin
@PerformanceSensitive("allocation")
public final class DynamicThresholdFilter extends AbstractFilter {
   private final Level defaultThreshold;
   private final String key;
   private final ContextDataInjector injector;
   private final Map<String, Level> levelMap;

   @Deprecated(since = "3.0.0", forRemoval = true)
   public static DynamicThresholdFilter createFilter(
      final String key, final KeyValuePair[] pairs, final Level defaultThreshold, final Filter.Result onMatch, final Filter.Result onMismatch
   ) {
      return newBuilder()
         .setKey(key)
         .setPairs(pairs)
         .setDefaultThreshold(defaultThreshold)
         .setOnMatch(onMatch)
         .setOnMismatch(onMismatch)
         .setContextDataInjector(ContextDataInjectorFactory.createInjector())
         .get();
   }

   @PluginFactory
   public static DynamicThresholdFilter.Builder newBuilder() {
      return new DynamicThresholdFilter.Builder();
   }

   private DynamicThresholdFilter(
      final String key,
      final Map<String, Level> pairs,
      final Level defaultLevel,
      final Filter.Result onMatch,
      final Filter.Result onMismatch,
      final ContextDataInjector injector
   ) {
      super(onMatch, onMismatch);
      StringMap map = ContextDataFactory.createContextData();
      LOGGER.debug("Successfully initialized ContextDataFactory by retrieving the context data with {} entries", map.size());
      Objects.requireNonNull(key, "key cannot be null");
      this.key = key;
      this.levelMap = pairs;
      this.defaultThreshold = defaultLevel;
      this.injector = injector;
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }

      if (!super.equalsImpl(obj)) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      DynamicThresholdFilter other = (DynamicThresholdFilter)obj;
      if (this.defaultThreshold == null) {
         if (other.defaultThreshold != null) {
            return false;
         }
      } else if (!this.defaultThreshold.equals(other.defaultThreshold)) {
         return false;
      }

      if (this.key == null) {
         if (other.key != null) {
            return false;
         }
      } else if (!this.key.equals(other.key)) {
         return false;
      }

      if (this.levelMap == null) {
         if (other.levelMap != null) {
            return false;
         }
      } else if (!this.levelMap.equals(other.levelMap)) {
         return false;
      }

      return true;
   }

   private Filter.Result filter(final Level level, final ReadOnlyStringMap contextMap) {
      String value = (String)contextMap.getValue(this.key);
      if (value != null) {
         Level ctxLevel = this.levelMap.get(value);
         if (ctxLevel == null) {
            ctxLevel = this.defaultThreshold;
         }

         return level.isMoreSpecificThan(ctxLevel) ? this.onMatch : this.onMismatch;
      } else {
         return Filter.Result.NEUTRAL;
      }
   }

   @Override
   public Filter.Result filter(final LogEvent event) {
      return this.filter(event.getLevel(), event.getContextData());
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Message msg, final Throwable t) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final Object msg, final Throwable t) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object... params) {
      return this.filter(level, this.currentContextData());
   }

   private ReadOnlyStringMap currentContextData() {
      return this.injector.rawContextData();
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(
      final Logger logger, final Level level, final Marker marker, final String msg, final Object p0, final Object p1, final Object p2, final Object p3
   ) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4
   ) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5
   ) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6
   ) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7
   ) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7,
      final Object p8
   ) {
      return this.filter(level, this.currentContextData());
   }

   @Override
   public Filter.Result filter(
      final Logger logger,
      final Level level,
      final Marker marker,
      final String msg,
      final Object p0,
      final Object p1,
      final Object p2,
      final Object p3,
      final Object p4,
      final Object p5,
      final Object p6,
      final Object p7,
      final Object p8,
      final Object p9
   ) {
      return this.filter(level, this.currentContextData());
   }

   public String getKey() {
      return this.key;
   }

   public Map<String, Level> getLevelMap() {
      return this.levelMap;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = super.hashCodeImpl();
      result = 31 * result + (this.defaultThreshold == null ? 0 : this.defaultThreshold.hashCode());
      result = 31 * result + (this.key == null ? 0 : this.key.hashCode());
      return 31 * result + (this.levelMap == null ? 0 : this.levelMap.hashCode());
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("key=").append(this.key);
      sb.append(", default=").append(this.defaultThreshold);
      if (this.levelMap.size() > 0) {
         sb.append('{');
         boolean first = true;

         for (Entry<String, Level> entry : this.levelMap.entrySet()) {
            if (!first) {
               sb.append(", ");
               first = false;
            }

            sb.append(entry.getKey()).append('=').append(entry.getValue());
         }

         sb.append('}');
      }

      return sb.toString();
   }

   public static class Builder extends AbstractFilter.AbstractFilterBuilder<DynamicThresholdFilter.Builder> implements Supplier<DynamicThresholdFilter> {
      private String key;
      private KeyValuePair[] pairs;
      private Level defaultThreshold;
      private ContextDataInjector contextDataInjector;

      public DynamicThresholdFilter.Builder setKey(@PluginAttribute final String key) {
         this.key = key;
         return this;
      }

      public DynamicThresholdFilter.Builder setPairs(@PluginElement final KeyValuePair[] pairs) {
         this.pairs = pairs;
         return this;
      }

      public DynamicThresholdFilter.Builder setDefaultThreshold(@PluginAttribute final Level defaultThreshold) {
         this.defaultThreshold = defaultThreshold;
         return this;
      }

      @Inject
      public DynamicThresholdFilter.Builder setContextDataInjector(final ContextDataInjector contextDataInjector) {
         this.contextDataInjector = contextDataInjector;
         return this;
      }

      public DynamicThresholdFilter get() {
         if (this.contextDataInjector == null) {
            this.contextDataInjector = ContextDataInjectorFactory.createInjector();
         }

         if (this.defaultThreshold == null) {
            this.defaultThreshold = Level.ERROR;
         }

         Map<String, Level> map = Stream.of(this.pairs).collect(Collectors.toMap(KeyValuePair::getKey, pair -> Level.toLevel(pair.getValue())));
         return new DynamicThresholdFilter(this.key, map, this.defaultThreshold, this.getOnMatch(), this.getOnMismatch(), this.contextDataInjector);
      }
   }
}
