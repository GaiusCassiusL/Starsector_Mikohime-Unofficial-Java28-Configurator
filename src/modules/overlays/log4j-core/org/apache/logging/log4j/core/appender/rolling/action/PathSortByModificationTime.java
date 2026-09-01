package org.apache.logging.log4j.core.appender.rolling.action;

import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(printObject = true)
@Plugin("SortByModificationTime")
public class PathSortByModificationTime implements PathSorter {
   private final boolean recentFirst;
   private final int multiplier;

   public PathSortByModificationTime(final boolean recentFirst) {
      this.recentFirst = recentFirst;
      this.multiplier = recentFirst ? 1 : -1;
   }

   @PluginFactory
   public static PathSorter createSorter(@PluginAttribute(defaultBoolean = true) final boolean recentFirst) {
      return new PathSortByModificationTime(recentFirst);
   }

   public boolean isRecentFirst() {
      return this.recentFirst;
   }

   public int compare(final PathWithAttributes path1, final PathWithAttributes path2) {
      long lastModified1 = path1.getAttributes().lastModifiedTime().toMillis();
      long lastModified2 = path2.getAttributes().lastModifiedTime().toMillis();
      int result = Long.signum(lastModified2 - lastModified1);
      if (result == 0) {
         try {
            result = path2.getPath().compareTo(path1.getPath());
         } catch (ClassCastException ex) {
            result = path2.getPath().toString().compareTo(path1.getPath().toString());
         }
      }

      return this.multiplier * result;
   }
}
