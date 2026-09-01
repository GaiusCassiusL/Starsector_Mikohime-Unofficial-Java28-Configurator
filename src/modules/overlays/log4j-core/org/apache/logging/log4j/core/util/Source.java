package org.apache.logging.log4j.core.util;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.Strings;

public class Source {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private final File file;
   private final URI uri;
   private final String location;

   private static String normalize(final File file) {
      try {
         return file.getCanonicalFile().getAbsolutePath();
      } catch (IOException e) {
         throw new IllegalArgumentException(e);
      }
   }

   private static File toFile(final Path path) {
      try {
         return Objects.requireNonNull(path, "path").toFile();
      } catch (UnsupportedOperationException e) {
         return null;
      }
   }

   private static File toFile(final URI uri) {
      try {
         String scheme = Objects.requireNonNull(uri, "uri").getScheme();
         if (!Strings.isBlank(scheme) && !scheme.equals("file")) {
            LOGGER.debug("uri does not represent a local file: " + uri);
            return null;
         } else {
            return new File(uri.getPath());
         }
      } catch (Exception e) {
         LOGGER.debug("uri is malformed: " + uri.toString());
         return null;
      }
   }

   private static URI toURI(final URL url) {
      try {
         return Objects.requireNonNull(url, "url").toURI();
      } catch (URISyntaxException e) {
         throw new IllegalArgumentException(e);
      }
   }

   public Source(final ConfigurationSource source) {
      this.file = source.getFile();
      this.uri = source.getURI();
      this.location = source.getLocation();
   }

   public Source(final File file) {
      this.file = Objects.requireNonNull(file, "file");
      this.location = normalize(file);
      this.uri = file.toURI();
   }

   public Source(final Path path) {
      Path normPath = Objects.requireNonNull(path, "path").normalize();
      this.file = toFile(normPath);
      this.uri = normPath.toUri();
      this.location = normPath.toString();
   }

   public Source(final URI uri) {
      URI normUri = Objects.requireNonNull(uri, "uri").normalize();
      this.uri = normUri;
      this.location = normUri.toString();
      this.file = toFile(normUri);
   }

   @Deprecated
   public Source(final URI uri, final long lastModified) {
      this(uri);
   }

   public Source(final URL url) {
      this.uri = toURI(url);
      this.location = this.uri.toString();
      this.file = toFile(this.uri);
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }

      if (!(obj instanceof Source)) {
         return false;
      }

      Source other = (Source)obj;
      return Objects.equals(this.location, other.location);
   }

   public File getFile() {
      return this.file;
   }

   public String getLocation() {
      return this.location;
   }

   public Path getPath() {
      return this.file != null ? this.file.toPath() : (this.uri != null ? Paths.get(this.uri) : Paths.get(this.location));
   }

   public URI getURI() {
      return this.uri;
   }

   public URL getURL() {
      try {
         return this.uri.toURL();
      } catch (MalformedURLException e) {
         throw new IllegalStateException(e);
      }
   }

   @Override
   public int hashCode() {
      return Objects.hash(this.location);
   }

   @Override
   public String toString() {
      return this.location;
   }
}
