package org.apache.logging.log4j.core.util;

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin
public final class KeyValuePair {
   private final String key;
   private final String value;

   public KeyValuePair(final String key, final String value) {
      this.key = key;
      this.value = value;
   }

   public String getKey() {
      return this.key;
   }

   public String getValue() {
      return this.value;
   }

   @Override
   public String toString() {
      return this.key + "=" + this.value;
   }

   @PluginFactory
   public static KeyValuePair.Builder newBuilder() {
      return new KeyValuePair.Builder();
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.key == null ? 0 : this.key.hashCode());
      return 31 * result + (this.value == null ? 0 : this.value.hashCode());
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      KeyValuePair other = (KeyValuePair)obj;
      if (this.key == null) {
         if (other.key != null) {
            return false;
         }
      } else if (!this.key.equals(other.key)) {
         return false;
      }

      if (this.value == null) {
         if (other.value != null) {
            return false;
         }
      } else if (!this.value.equals(other.value)) {
         return false;
      }

      return true;
   }

   public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<KeyValuePair> {
      @PluginBuilderAttribute
      private String key;
      @PluginBuilderAttribute
      private String value;

      public KeyValuePair.Builder setKey(final String aKey) {
         this.key = aKey;
         return this;
      }

      public KeyValuePair.Builder setValue(final String aValue) {
         this.value = aValue;
         return this;
      }

      public KeyValuePair build() {
         return new KeyValuePair(this.key, this.value);
      }
   }
}
