package org.apache.logging.log4j.core.util;

import java.io.File;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationListener;
import org.apache.logging.log4j.core.config.Reconfigurable;

public class WrappedFileWatcher extends AbstractWatcher implements FileWatcher {
   private final FileWatcher watcher;
   private volatile long lastModifiedMillis;

   public WrappedFileWatcher(
      final FileWatcher watcher,
      final Configuration configuration,
      final Reconfigurable reconfigurable,
      final List<ConfigurationListener> configurationListeners,
      final long lastModifiedMillis
   ) {
      super(configuration, reconfigurable, configurationListeners);
      this.watcher = watcher;
      this.lastModifiedMillis = lastModifiedMillis;
   }

   public WrappedFileWatcher(final FileWatcher watcher) {
      super(null, null, null);
      this.watcher = watcher;
   }

   @Override
   public long getLastModified() {
      return this.lastModifiedMillis;
   }

   @Override
   public void fileModified(final File file) {
      this.watcher.fileModified(file);
   }

   @Override
   public boolean isModified() {
      long lastModified = this.getSource().getFile().lastModified();
      if (this.lastModifiedMillis != lastModified) {
         this.lastModifiedMillis = lastModified;
         return true;
      } else {
         return false;
      }
   }

   @Override
   public List<ConfigurationListener> getListeners() {
      return super.getListeners() != null ? Collections.unmodifiableList(super.getListeners()) : null;
   }

   @Override
   public void modified() {
      if (this.getListeners() != null) {
         super.modified();
      }

      this.fileModified(this.getSource().getFile());
      this.lastModifiedMillis = this.getSource().getFile().lastModified();
   }

   @Override
   public void watching(final Source source) {
      this.lastModifiedMillis = source.getFile().lastModified();
      super.watching(source);
   }

   @Override
   public Watcher newWatcher(final Reconfigurable reconfigurable, final List<ConfigurationListener> listeners, final long lastModifiedMillis) {
      WrappedFileWatcher watcher = new WrappedFileWatcher(this.watcher, this.getConfiguration(), reconfigurable, listeners, lastModifiedMillis);
      if (this.getSource() != null) {
         watcher.watching(this.getSource());
      }

      return watcher;
   }
}
