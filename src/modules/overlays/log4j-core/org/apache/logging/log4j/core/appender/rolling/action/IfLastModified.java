package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.time.Clock;
import org.apache.logging.log4j.core.time.ClockFactory;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Inject;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.status.StatusLogger;

@Configurable(printObject = true)
@Plugin
public final class IfLastModified implements PathCondition {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final Clock clock;
   private final Duration age;
   private final PathCondition[] nestedConditions;

   private IfLastModified(final Duration age, final PathCondition[] nestedConditions, final Clock clock) {
      this.age = Objects.requireNonNull(age, "age");
      this.nestedConditions = PathCondition.copy(nestedConditions);
      this.clock = clock;
   }

   public Duration getAge() {
      return this.age;
   }

   public List<PathCondition> getNestedConditions() {
      return List.of(this.nestedConditions);
   }

   @Override
   public boolean accept(final Path basePath, final Path relativePath, final BasicFileAttributes attrs) {
      FileTime fileTime = attrs.lastModifiedTime();
      long millis = fileTime.toMillis();
      long ageMillis = this.clock.currentTimeMillis() - millis;
      boolean result = ageMillis >= this.age.toMillis();
      String match = result ? ">=" : "<";
      String accept = result ? "ACCEPTED" : "REJECTED";
      LOGGER.trace("IfLastModified {}: {} ageMillis '{}' {} '{}'", accept, relativePath, ageMillis, match, this.age);
      return result ? IfAll.accept(this.nestedConditions, basePath, relativePath, attrs) : result;
   }

   @Override
   public void beforeFileTreeWalk() {
      IfAll.beforeFileTreeWalk(this.nestedConditions);
   }

   @Override
   public String toString() {
      String nested = this.nestedConditions.length == 0 ? "" : " AND " + Arrays.toString(this.nestedConditions);
      return "IfLastModified(age=" + this.age + nested + ")";
   }

   @Deprecated(since = "3.0.0", forRemoval = true)
   public static IfLastModified createAgeCondition(final Duration age, final PathCondition... nestedConditions) {
      return newBuilder().setAge(age).setNestedConditions(nestedConditions).get();
   }

   @PluginFactory
   public static IfLastModified.Builder newBuilder() {
      return new IfLastModified.Builder();
   }

   public static class Builder implements Supplier<IfLastModified> {
      private Duration age;
      private PathCondition[] nestedConditions;
      private Clock clock;

      public IfLastModified.Builder setAge(@PluginAttribute final Duration age) {
         this.age = age;
         return this;
      }

      public IfLastModified.Builder setNestedConditions(@PluginElement final PathCondition... nestedConditions) {
         this.nestedConditions = nestedConditions;
         return this;
      }

      @Inject
      public IfLastModified.Builder setClock(final Clock clock) {
         this.clock = clock;
         return this;
      }

      public IfLastModified get() {
         if (this.clock == null) {
            this.clock = ClockFactory.getClock();
         }

         return new IfLastModified(this.age, this.nestedConditions, this.clock);
      }
   }
}
