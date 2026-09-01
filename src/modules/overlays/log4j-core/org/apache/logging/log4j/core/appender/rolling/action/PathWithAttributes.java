package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class PathWithAttributes {
   private final Path path;
   private final BasicFileAttributes attributes;

   public PathWithAttributes(final Path path, final BasicFileAttributes attributes) {
      this.path = Objects.requireNonNull(path, "path");
      this.attributes = Objects.requireNonNull(attributes, "attributes");
   }

   @Override
   public String toString() {
      return this.path + " (modified: " + this.attributes.lastModifiedTime() + ")";
   }

   public Path getPath() {
      return this.path;
   }

   public BasicFileAttributes getAttributes() {
      return this.attributes;
   }
}
