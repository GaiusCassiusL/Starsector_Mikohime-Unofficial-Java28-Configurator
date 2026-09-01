package org.apache.logging.log4j.core.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import javax.net.ssl.HttpsURLConnection;
import org.apache.logging.log4j.core.net.ssl.LaxHostnameVerifier;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.net.ssl.SslConfigurationFactory;
import org.apache.logging.log4j.core.util.AuthorizationProvider;
import org.apache.logging.log4j.core.util.FileUtils;
import org.apache.logging.log4j.core.util.Loader;
import org.apache.logging.log4j.core.util.Source;
import org.apache.logging.log4j.util.LoaderUtil;
import org.apache.logging.log4j.util.PropertiesUtil;

public class ConfigurationSource {
   public static final ConfigurationSource NULL_SOURCE = new ConfigurationSource(new byte[0], null, 0L);
   public static final ConfigurationSource COMPOSITE_SOURCE = new ConfigurationSource(new byte[0], null, 0L);
   private static final String HTTPS = "https";
   private static final String HTTP = "http";
   private final InputStream stream;
   private volatile byte[] data;
   private volatile Source source;
   private final long lastModified;
   private volatile long modifiedMillis;

   public ConfigurationSource(final InputStream stream, final File file) {
      this.stream = Objects.requireNonNull(stream, "stream is null");
      this.data = null;
      this.source = new Source(file);
      long modified = 0L;

      try {
         modified = file.lastModified();
      } catch (Exception var6) {
      }

      this.lastModified = modified;
   }

   public ConfigurationSource(final InputStream stream, final Path path) {
      this.stream = Objects.requireNonNull(stream, "stream is null");
      this.data = null;
      this.source = new Source(path);
      long modified = 0L;

      try {
         modified = Files.getLastModifiedTime(path).toMillis();
      } catch (Exception var6) {
      }

      this.lastModified = modified;
   }

   public ConfigurationSource(final InputStream stream, final URL url) {
      this.stream = Objects.requireNonNull(stream, "stream is null");
      this.data = null;
      this.lastModified = 0L;
      this.source = new Source(url);
   }

   public ConfigurationSource(final InputStream stream, final URL url, final long lastModified) {
      this.stream = Objects.requireNonNull(stream, "stream is null");
      this.data = null;
      this.lastModified = lastModified;
      this.source = new Source(url);
   }

   public ConfigurationSource(final InputStream stream) throws IOException {
      this(stream.readAllBytes(), null, 0L);
   }

   public ConfigurationSource(final Source source, final byte[] data, final long lastModified) throws IOException {
      Objects.requireNonNull(source, "source is null");
      this.data = Objects.requireNonNull(data, "data is null");
      this.stream = new ByteArrayInputStream(data);
      this.lastModified = lastModified;
      this.source = source;
   }

   private ConfigurationSource(final byte[] data, final URL url, final long lastModified) {
      this.data = Objects.requireNonNull(data, "data is null");
      this.stream = new ByteArrayInputStream(data);
      this.lastModified = lastModified;
      if (url == null) {
         this.data = data;
      } else {
         this.source = new Source(url);
      }
   }

   public File getFile() {
      return this.source == null ? null : this.source.getFile();
   }

   private boolean isFile() {
      return this.source == null ? false : this.source.getFile() != null;
   }

   private boolean isURL() {
      return this.source == null ? false : this.source.getURI() != null;
   }

   private boolean isLocation() {
      return this.source == null ? false : this.source.getLocation() != null;
   }

   public URL getURL() {
      return this.source == null ? null : this.source.getURL();
   }

   @Deprecated
   public void setSource(final Source source) {
      this.source = source;
   }

   public void setData(final byte[] data) {
      this.data = data;
   }

   public void setModifiedMillis(final long modifiedMillis) {
      this.modifiedMillis = modifiedMillis;
   }

   public URI getURI() {
      return this.source == null ? null : this.source.getURI();
   }

   public long getLastModified() {
      return this.lastModified;
   }

   public String getLocation() {
      return this.source == null ? null : this.source.getLocation();
   }

   public InputStream getInputStream() {
      return this.stream;
   }

   public ConfigurationSource resetInputStream() throws IOException {
      if (this.source != null && this.data != null) {
         return new ConfigurationSource(this.source, this.data, this.lastModified);
      } else if (this.isFile()) {
         return new ConfigurationSource(new FileInputStream(this.getFile()), this.getFile());
      } else if (this.isURL() && this.data != null) {
         return new ConfigurationSource(this.data, this.getURL(), this.modifiedMillis == 0L ? this.lastModified : this.modifiedMillis);
      } else if (this.isURL()) {
         return fromUri(this.getURI());
      } else {
         return this.data != null ? new ConfigurationSource(this.data, null, this.lastModified) : null;
      }
   }

   @Override
   public String toString() {
      if (this.isLocation()) {
         return this.getLocation();
      }

      if (this == NULL_SOURCE) {
         return "NULL_SOURCE";
      }

      if (this == COMPOSITE_SOURCE) {
         return "COMPOSITE_SOURCE";
      }

      int length = this.data == null ? -1 : this.data.length;
      return "stream (" + length + " bytes, unknown location)";
   }

   public static ConfigurationSource fromUri(final URI configLocation) {
      File configFile = FileUtils.fileFromUri(configLocation);
      if (configFile != null && configFile.exists() && configFile.canRead()) {
         try {
            return new ConfigurationSource(new FileInputStream(configFile), configFile);
         } catch (FileNotFoundException ex) {
            ConfigurationFactory.LOGGER.error("Cannot locate file {}", configLocation.getPath(), ex);
         }
      }

      if (ConfigurationFactory.isClassLoaderUri(configLocation)) {
         ClassLoader loader = LoaderUtil.getThreadContextClassLoader();
         String path = ConfigurationFactory.extractClassLoaderUriPath(configLocation);
         return fromResource(path, loader);
      }

      if (!configLocation.isAbsolute()) {
         ConfigurationFactory.LOGGER.error("File not found in file system or classpath: {}", configLocation.toString());
         return null;
      }

      try {
         return getConfigurationSource(configLocation.toURL());
      } catch (MalformedURLException ex) {
         ConfigurationFactory.LOGGER.error("Invalid URL {}", configLocation.toString(), ex);
         return null;
      }
   }

   public static ConfigurationSource fromResource(final String resource, final ClassLoader loader) {
      URL url = Loader.getResource(resource, loader);
      return url == null ? null : getConfigurationSource(url);
   }

   private static ConfigurationSource getConfigurationSource(final URL url) {
      try {
         URLConnection urlConnection = url.openConnection();
         urlConnection.setUseCaches(false);
         AuthorizationProvider provider = ConfigurationFactory.authorizationProvider(PropertiesUtil.getProperties());
         provider.addAuthorization(urlConnection);
         if (url.getProtocol().equals("https")) {
            SslConfiguration sslConfiguration = SslConfigurationFactory.getSslConfiguration();
            if (sslConfiguration != null) {
               ((HttpsURLConnection)urlConnection).setSSLSocketFactory(sslConfiguration.getSslSocketFactory());
               if (!sslConfiguration.isVerifyHostName()) {
                  ((HttpsURLConnection)urlConnection).setHostnameVerifier(LaxHostnameVerifier.INSTANCE);
               }
            }
         }

         File file = FileUtils.fileFromUri(url.toURI());

         try {
            if (file != null) {
               return new ConfigurationSource(urlConnection.getInputStream(), FileUtils.fileFromUri(url.toURI()));
            } else if (urlConnection instanceof JarURLConnection) {
               URL jarFileUrl = ((JarURLConnection)urlConnection).getJarFileURL();
               File jarFile = new File(jarFileUrl.getFile());
               long lastModified = jarFile.lastModified();
               return new ConfigurationSource(urlConnection.getInputStream(), url, lastModified);
            } else {
               return new ConfigurationSource(urlConnection.getInputStream(), url, urlConnection.getLastModified());
            }
         } catch (FileNotFoundException ex) {
            ConfigurationFactory.LOGGER.info("Unable to locate file {}, ignoring.", url.toString());
            return null;
         }
      } catch (IOException | URISyntaxException ex) {
         ConfigurationFactory.LOGGER.warn("Error accessing {} due to {}, ignoring.", url.toString(), ex.getMessage());
         return null;
      }
   }
}
