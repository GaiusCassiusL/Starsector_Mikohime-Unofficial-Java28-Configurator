package org.apache.logging.log4j.core.util.internal;

import java.io.File;
import java.net.URI;
import org.apache.logging.log4j.core.util.Source;

public class LastModifiedSource extends Source {
   private volatile long lastModified;

   public LastModifiedSource(final File file) {
      super(file);
      this.lastModified = 0L;
   }

   public LastModifiedSource(final URI uri) {
      this(uri, 0L);
   }

   public LastModifiedSource(final URI uri, final long lastModifiedMillis) {
      super(uri);
      this.lastModified = lastModifiedMillis;
   }

   public long getLastModified() {
      return this.lastModified;
   }

   public void setLastModified(long lastModified) {
      this.lastModified = lastModified;
   }
}
