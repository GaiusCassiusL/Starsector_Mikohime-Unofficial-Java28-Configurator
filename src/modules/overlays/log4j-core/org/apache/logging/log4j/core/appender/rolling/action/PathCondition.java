package org.apache.logging.log4j.core.appender.rolling.action;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;

public interface PathCondition {
   PathCondition[] EMPTY_ARRAY = new PathCondition[0];

   static PathCondition[] copy(PathCondition... source) {
      return source != null && source.length != 0 ? Arrays.copyOf(source, source.length) : EMPTY_ARRAY;
   }

   void beforeFileTreeWalk();

   boolean accept(final Path baseDir, final Path relativePath, final BasicFileAttributes attrs);
}
