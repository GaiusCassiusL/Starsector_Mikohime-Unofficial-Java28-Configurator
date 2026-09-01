package org.apache.log4j.xml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import javax.xml.parsers.FactoryConfigurationError;
import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.spi.LoggerRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.net.UrlConnectionFactory;
import org.apache.logging.log4j.core.util.IOUtils;
import org.w3c.dom.Element;

public class DOMConfigurator {
   public static void configure(final Element element) {
   }

   public static void configure(final String fileName) throws FactoryConfigurationError {
      Path path = Paths.get(fileName);

      try (InputStream inputStream = Files.newInputStream(path)) {
         ConfigurationSource source = new ConfigurationSource(inputStream, path);
         LoggerContext context = (LoggerContext)LogManager.getContext(false);
         Configuration configuration = new XmlConfigurationFactory().getConfiguration(context, source);
         org.apache.log4j.LogManager.getRootLogger().removeAllAppenders();
         Configurator.reconfigure(configuration);
      } catch (IOException e) {
         throw new FactoryConfigurationError(e);
      }
   }

   public static void configure(final URL url) throws FactoryConfigurationError {
      new DOMConfigurator().doConfigure(url, org.apache.log4j.LogManager.getLoggerRepository());
   }

   public static void configureAndWatch(final String fileName) {
      configure(fileName);
   }

   public static void configureAndWatch(final String fileName, final long delay) {
      XMLWatchdog xdog = new XMLWatchdog(fileName);
      xdog.setDelay(delay);
      xdog.start();
   }

   public static Object parseElement(final Element element, final Properties props, final Class expectedClass) {
      return null;
   }

   public static void setParameter(final Element elem, final PropertySetter propSetter, final Properties props) {
   }

   public static String subst(final String value, final Properties props) {
      return OptionConverter.substVars(value, props);
   }

   private void doConfigure(final ConfigurationSource source) {
      LoggerContext context = (LoggerContext)LogManager.getContext(false);
      Configuration configuration = new XmlConfigurationFactory().getConfiguration(context, source);
      Configurator.reconfigure(configuration);
   }

   public void doConfigure(final Element element, final LoggerRepository repository) {
   }

   public void doConfigure(final InputStream inputStream, final LoggerRepository repository) throws FactoryConfigurationError {
      try {
         this.doConfigure(new ConfigurationSource(inputStream));
      } catch (IOException e) {
         throw new FactoryConfigurationError(e);
      }
   }

   public void doConfigure(final Reader reader, final LoggerRepository repository) throws FactoryConfigurationError {
      try {
         StringWriter sw = new StringWriter();
         IOUtils.copy(reader, sw);
         this.doConfigure(new ConfigurationSource(new ByteArrayInputStream(sw.toString().getBytes(StandardCharsets.UTF_8))));
      } catch (IOException e) {
         throw new FactoryConfigurationError(e);
      }
   }

   public void doConfigure(final String fileName, final LoggerRepository repository) {
      configure(fileName);
   }

   public void doConfigure(final URL url, final LoggerRepository repository) {
      try {
         URLConnection connection = UrlConnectionFactory.createConnection(url);

         try (InputStream inputStream = connection.getInputStream()) {
            this.doConfigure(new ConfigurationSource(inputStream, url));
         }
      } catch (IOException e) {
         throw new FactoryConfigurationError(e);
      }
   }
}
