package org.apache.logging.log4j.core.config.builder.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.builder.api.Component;
import org.apache.logging.log4j.core.config.builder.api.ComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;

class DefaultComponentBuilder<T extends ComponentBuilder<T>, CB extends ConfigurationBuilder<? extends Configuration>> implements ComponentBuilder<T> {
   private final CB builder;
   private final String type;
   private final Map<String, String> attributes = new LinkedHashMap<>();
   private final List<Component> components = new ArrayList<>();
   private final String name;
   private final String value;

   public DefaultComponentBuilder(final CB builder, final String type) {
      this(builder, null, type, null);
   }

   public DefaultComponentBuilder(final CB builder, final String name, final String type) {
      this(builder, name, type, null);
   }

   public DefaultComponentBuilder(final CB builder, final String name, final String type, final String value) {
      this.type = type;
      this.builder = builder;
      this.name = name;
      this.value = value;
   }

   @Override
   public T addAttribute(final String key, final boolean value) {
      return this.put(key, Boolean.toString(value));
   }

   @Override
   public T addAttribute(final String key, final Enum<?> value) {
      return this.put(key, value.name());
   }

   @Override
   public T addAttribute(final String key, final int value) {
      return this.put(key, Integer.toString(value));
   }

   @Override
   public T addAttribute(final String key, final Level level) {
      return this.put(key, level.toString());
   }

   @Override
   public T addAttribute(final String key, final Object value) {
      return this.put(key, value.toString());
   }

   @Override
   public T addAttribute(final String key, final String value) {
      return this.put(key, value);
   }

   @Override
   public T addComponent(final ComponentBuilder<?> builder) {
      this.components.add((Component)builder.build());
      return (T)this;
   }

   public Component build() {
      Component component = new Component(this.type, this.name, this.value);
      component.getAttributes().putAll(this.attributes);
      component.getComponents().addAll(this.components);
      return component;
   }

   @Override
   public CB getBuilder() {
      return this.builder;
   }

   @Override
   public String getName() {
      return this.name;
   }

   protected T put(final String key, final String value) {
      this.attributes.put(key, value);
      return (T)this;
   }
}
