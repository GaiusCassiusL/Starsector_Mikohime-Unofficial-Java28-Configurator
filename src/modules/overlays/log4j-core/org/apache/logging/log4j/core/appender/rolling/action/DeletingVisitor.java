package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public class DeletingVisitor extends SimpleFileVisitor<Path> {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final Path basePath;
   private final boolean testMode;
   private final List<? extends PathCondition> pathConditions;

   public DeletingVisitor(final Path basePath, final List<? extends PathCondition> pathConditions, final boolean testMode) {
      this.testMode = testMode;
      this.basePath = Objects.requireNonNull(basePath, "basePath");
      this.pathConditions = Objects.requireNonNull(pathConditions, "pathConditions");

      for (PathCondition condition : pathConditions) {
         condition.beforeFileTreeWalk();
      }
   }

   public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
      for (PathCondition pathFilter : this.pathConditions) {
         Path relative = this.basePath.relativize(file);
         if (!pathFilter.accept(this.basePath, relative, attrs)) {
            LOGGER.trace("Not deleting base={}, relative={}", this.basePath, relative);
            return FileVisitResult.CONTINUE;
         }
      }

      if (this.isTestMode()) {
         LOGGER.info("Deleting {} (TEST MODE: file not actually deleted)", file);
      } else {
         this.delete(file);
      }

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

   protected void delete(final Path file) throws IOException {
      LOGGER.trace("Deleting {}", file);
      Files.deleteIfExists(file);
   }

   public boolean isTestMode() {
      return this.testMode;
   }
}
