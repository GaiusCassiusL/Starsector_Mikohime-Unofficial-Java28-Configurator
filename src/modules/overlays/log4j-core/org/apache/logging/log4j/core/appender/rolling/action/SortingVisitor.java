package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public class SortingVisitor extends SimpleFileVisitor<Path> {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final PathSorter sorter;
   private final List<PathWithAttributes> collected = new ArrayList<>();

   public SortingVisitor(final PathSorter sorter) {
      this.sorter = Objects.requireNonNull(sorter, "sorter");
   }

   public FileVisitResult visitFile(final Path path, final BasicFileAttributes attrs) throws IOException {
      this.collected.add(new PathWithAttributes(path, attrs));
      return FileVisitResult.CONTINUE;
   }

   public FileVisitResult visitFileFailed(final Path file, final IOException ioException) throws IOException {
      if (ioException instanceof NoSuchFileException) {
         LOGGER.info("File {} could not be accessed, it has likely already been deleted", file, ioException);
         return FileVisitResult.CONTINUE;
      } else {
         return super.visitFileFailed(file, ioException);
      }
   }

   public List<PathWithAttributes> getSortedPaths() {
      Collections.sort(this.collected, this.sorter);
      return this.collected;
   }
}
