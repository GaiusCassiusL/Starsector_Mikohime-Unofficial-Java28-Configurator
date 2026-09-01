package org.apache.logging.log4j.core.config.plugins.visit;

import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Named;
import org.apache.logging.log4j.plugins.Node;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.di.Injector;
import org.apache.logging.log4j.plugins.di.Keys;
import org.apache.logging.log4j.plugins.visit.NodeVisitor;
import org.apache.logging.log4j.util.StringBuilders;
import org.apache.logging.log4j.util.Strings;

public class PluginAttributeVisitor implements NodeVisitor {
   private static final Map<Type, Function<PluginAttribute, ?>> DEFAULT_VALUE_EXTRACTORS = Map.ofEntries(
      Map.entry(int.class, PluginAttribute::defaultInt),
      Map.entry(Integer.class, PluginAttribute::defaultInt),
      Map.entry(long.class, PluginAttribute::defaultLong),
      Map.entry(Long.class, PluginAttribute::defaultLong),
      Map.entry(boolean.class, PluginAttribute::defaultBoolean),
      Map.entry(Boolean.class, PluginAttribute::defaultBoolean),
      Map.entry(float.class, PluginAttribute::defaultFloat),
      Map.entry(Float.class, PluginAttribute::defaultFloat),
      Map.entry(double.class, PluginAttribute::defaultDouble),
      Map.entry(Double.class, PluginAttribute::defaultDouble),
      Map.entry(byte.class, PluginAttribute::defaultByte),
      Map.entry(Byte.class, PluginAttribute::defaultByte),
      Map.entry(char.class, PluginAttribute::defaultChar),
      Map.entry(Character.class, PluginAttribute::defaultChar),
      Map.entry(short.class, PluginAttribute::defaultShort),
      Map.entry(Short.class, PluginAttribute::defaultShort),
      Map.entry(Class.class, PluginAttribute::defaultClass)
   );
   private final Function<String, String> stringSubstitutionStrategy;
   private final Injector injector;

   @Inject
   public PluginAttributeVisitor(@Named("StringSubstitutor") final Function<String, String> stringSubstitutionStrategy, final Injector injector) {
      this.stringSubstitutionStrategy = stringSubstitutionStrategy;
      this.injector = injector;
   }

   public Object visitField(final Field field, final Node node, final StringBuilder debugLog) {
      String name = Keys.getName(field);
      Collection<String> aliases = Keys.getAliases(field);
      Type targetType = field.getGenericType();
      TypeConverter<?> converter = this.injector.getTypeConverter(targetType);
      PluginAttribute annotation = field.getAnnotation(PluginAttribute.class);
      boolean sensitive = annotation.sensitive();
      Object value = node.removeMatchingAttribute(name, aliases)
         .map(this.stringSubstitutionStrategy.andThen(s -> converter.convert(s, null, sensitive)))
         .orElseGet(() -> this.getDefaultValue(targetType, annotation));
      StringBuilders.appendKeyDqValueWithJoiner(debugLog, name, sensitive ? "(***)" : value, ", ");
      return value;
   }

   public Object visitParameter(final Parameter parameter, final Node node, final StringBuilder debugLog) {
      String name = Keys.getName(parameter);
      Collection<String> aliases = Keys.getAliases(parameter);
      Type targetType = parameter.getParameterizedType();
      TypeConverter<?> converter = this.injector.getTypeConverter(targetType);
      PluginAttribute annotation = parameter.getAnnotation(PluginAttribute.class);
      boolean sensitive = annotation.sensitive();
      Object value = node.removeMatchingAttribute(name, aliases)
         .map(this.stringSubstitutionStrategy.andThen(s -> converter.convert(s, null, sensitive)))
         .orElseGet(() -> this.getDefaultValue(targetType, annotation));
      StringBuilders.appendKeyDqValueWithJoiner(debugLog, name, sensitive ? "(***)" : value, ", ");
      return value;
   }

   private Object getDefaultValue(final Type targetType, final PluginAttribute annotation) {
      Function<PluginAttribute, ?> extractor = DEFAULT_VALUE_EXTRACTORS.get(targetType);
      if (extractor != null) {
         return extractor.apply(annotation);
      }

      TypeConverter<?> converter = this.injector.getTypeConverter(targetType);
      String value = this.stringSubstitutionStrategy.apply(annotation.defaultString());
      return Strings.isEmpty(value) ? null : converter.convert(value, null);
   }
}
