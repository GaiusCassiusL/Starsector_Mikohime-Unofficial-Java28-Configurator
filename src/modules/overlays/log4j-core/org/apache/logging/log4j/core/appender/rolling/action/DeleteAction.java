package org.apache.logging.log4j.core.appender.rolling.action;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.apache.logging.log4j.core.script.ScriptConditional;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin("Delete")
public class DeleteAction extends AbstractPathAction {
   private final PathSorter pathSorter;
   private final boolean testMode;
   private final ScriptConditional scriptCondition;

   DeleteAction(
      final String basePath,
      final boolean followSymbolicLinks,
      final int maxDepth,
      final boolean testMode,
      final PathSorter sorter,
      final PathCondition[] pathConditions,
      final ScriptConditional scriptCondition,
      final StrSubstitutor subst
   ) {
      super(basePath, followSymbolicLinks, maxDepth, pathConditions, subst);
      this.testMode = testMode;
      this.pathSorter = Objects.requireNonNull(sorter, "sorter");
      this.scriptCondition = scriptCondition;
      if (scriptCondition == null && (pathConditions == null || pathConditions.length == 0)) {
         LOGGER.error("Missing Delete conditions: unconditional Delete not supported");
         throw new IllegalArgumentException("Unconditional Delete not supported");
      }
   }

   @Override
   public boolean execute() throws IOException {
      return this.scriptCondition != null ? this.executeScript() : super.execute();
   }

   private boolean executeScript() throws IOException {
      List<PathWithAttributes> selectedForDeletion = this.callScript();
      if (selectedForDeletion == null) {
         LOGGER.trace("Script returned null list (no files to delete)");
         return true;
      } else {
         this.deleteSelectedFiles(selectedForDeletion);
         return true;
      }
   }

   private List<PathWithAttributes> callScript() throws IOException {
      List<PathWithAttributes> sortedPaths = this.getSortedPaths();
      this.trace("Sorted paths:", sortedPaths);
      return this.scriptCondition.selectFilesToDelete(this.getBasePath(), sortedPaths);
   }

   private void deleteSelectedFiles(final List<PathWithAttributes> selectedForDeletion) throws IOException {
      this.trace("Paths the script selected for deletion:", selectedForDeletion);

      for (PathWithAttributes pathWithAttributes : selectedForDeletion) {
         Path path = pathWithAttributes == null ? null : pathWithAttributes.getPath();
         if (this.isTestMode()) {
            LOGGER.info("Deleting {} (TEST MODE: file not actually deleted)", path);
         } else {
            this.delete(path);
         }
      }
   }

   protected void delete(final Path path) throws IOException {
      LOGGER.trace("Deleting {}", path);
      Files.deleteIfExists(path);
   }

   @Override
   public boolean execute(final FileVisitor<Path> visitor) throws IOException {
      List<PathWithAttributes> sortedPaths = this.getSortedPaths();
      this.trace("Sorted paths:", sortedPaths);

      for (PathWithAttributes element : sortedPaths) {
         try {
            visitor.visitFile(element.getPath(), element.getAttributes());
         } catch (IOException ioex) {
            LOGGER.error("Error in post-rollover Delete when visiting {}", element.getPath(), ioex);
            visitor.visitFileFailed(element.getPath(), ioex);
         }
      }

      return true;
   }

   private void trace(final String label, final List<PathWithAttributes> sortedPaths) {
      LOGGER.trace(label);

      for (PathWithAttributes pathWithAttributes : sortedPaths) {
         LOGGER.trace(pathWithAttributes);
      }
   }

   List<PathWithAttributes> getSortedPaths() throws IOException {
      SortingVisitor sort = new SortingVisitor(this.pathSorter);
      super.execute(sort);
      return sort.getSortedPaths();
   }

   public boolean isTestMode() {
      return this.testMode;
   }

   @Override
   protected FileVisitor<Path> createFileVisitor(final Path visitorBaseDir, final List<PathCondition> conditions) {
      return new DeletingVisitor(visitorBaseDir, conditions, this.testMode);
   }

   @PluginFactory
   public static DeleteAction createDeleteAction(
      @PluginAttribute final String basePath,
      @PluginAttribute final boolean followLinks,
      @PluginAttribute(defaultInt = 1) final int maxDepth,
      @PluginAttribute final boolean testMode,
      @PluginElement final PathSorter sorterParameter,
      @PluginElement final PathCondition[] pathConditions,
      @PluginElement final ScriptConditional scriptCondition,
      @PluginConfiguration final Configuration config
   ) {
      PathSorter sorter = sorterParameter == null ? new PathSortByModificationTime(true) : sorterParameter;
      return new DeleteAction(basePath, followLinks, maxDepth, testMode, sorter, pathConditions, scriptCondition, config.getStrSubstitutor());
   }
}
