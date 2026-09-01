package org.apache.logging.log4j.core.layout;

import java.io.ObjectStreamException;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin
public final class PatternMatch {
   private final String key;
   private final String pattern;

   public PatternMatch(final String key, final String pattern) {
      this.key = key;
      this.pattern = pattern;
   }

   public String getKey() {
      return this.key;
   }

   public String getPattern() {
      return this.pattern;
   }

   @Override
   public String toString() {
      return this.key + "=" + this.pattern;
   }

   @PluginFactory
   public static PatternMatch.Builder newBuilder() {
      return new PatternMatch.Builder();
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.key == null ? 0 : this.key.hashCode());
      return 31 * result + (this.pattern == null ? 0 : this.pattern.hashCode());
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

      PatternMatch other = (PatternMatch)obj;
      if (this.key == null) {
         if (other.key != null) {
            return false;
         }
      } else if (!this.key.equals(other.key)) {
         return false;
      }

      if (this.pattern == null) {
         if (other.pattern != null) {
            return false;
         }
      } else if (!this.pattern.equals(other.pattern)) {
         return false;
      }

      return true;
   }

   public static class Builder implements org.apache.logging.log4j.plugins.util.Builder<PatternMatch> {
      @PluginBuilderAttribute
      private String key;
      @PluginBuilderAttribute
      private String pattern;

      public PatternMatch.Builder setKey(final String key) {
         this.key = key;
         return this;
      }

      public PatternMatch.Builder setPattern(final String pattern) {
         this.pattern = pattern;
         return this;
      }

      public PatternMatch build() {
         return new PatternMatch(this.key, this.pattern);
      }

      protected Object readResolve() throws ObjectStreamException {
         return new PatternMatch(this.key, this.pattern);
      }
   }
}
