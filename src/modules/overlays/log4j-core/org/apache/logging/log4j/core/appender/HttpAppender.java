package org.apache.logging.log4j.core.appender;

import java.net.URL;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.plugins.validation.constraints.Required;

@Configurable(elementType = "appender", printObject = true)
@Plugin("Http")
public final class HttpAppender extends AbstractAppender {
   private final HttpManager manager;

   @PluginFactory
   public static <B extends HttpAppender.Builder<B>> B newBuilder() {
      return new HttpAppender.Builder<B>().asBuilder();
   }

   private HttpAppender(
      final String name, final Layout layout, final Filter filter, final boolean ignoreExceptions, final HttpManager manager, final Property[] properties
   ) {
      super(name, filter, layout, ignoreExceptions, properties);
      Objects.requireNonNull(layout, "layout");
      this.manager = Objects.requireNonNull(manager, "manager");
   }

   @Override
   public void start() {
      super.start();
      this.manager.startup();
   }

   @Override
   public void append(final LogEvent event) {
      try {
         this.manager.send(this.getLayout(), event);
      } catch (Exception e) {
         this.error("Unable to send HTTP in appender [" + this.getName() + "]", event, e);
      }
   }

   @Override
   public boolean stop(final long timeout, final TimeUnit timeUnit) {
      this.setStopping();
      boolean stopped = super.stop(timeout, timeUnit, false);
      stopped &= this.manager.stop(timeout, timeUnit);
      this.setStopped();
      return stopped;
   }

   @Override
   public String toString() {
      return "HttpAppender{name=" + this.getName() + ", state=" + this.getState() + "}";
   }

   public static class Builder<B extends HttpAppender.Builder<B>>
      extends AbstractAppender.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<HttpAppender> {
      @PluginBuilderAttribute
      @Required(message = "No URL provided for HttpAppender")
      private URL url;
      @PluginBuilderAttribute
      private String method = "POST";
      @PluginBuilderAttribute
      private int connectTimeoutMillis = 0;
      @PluginBuilderAttribute
      private int readTimeoutMillis = 0;
      @PluginElement("Headers")
      private Property[] headers;
      @PluginElement("SslConfiguration")
      private SslConfiguration sslConfiguration;
      @PluginBuilderAttribute
      private boolean verifyHostname = true;

      public HttpAppender build() {
         HttpManager httpManager = new HttpURLConnectionManager(
            this.getConfiguration(),
            this.getConfiguration().getLoggerContext(),
            this.getName(),
            this.url,
            this.method,
            this.connectTimeoutMillis,
            this.readTimeoutMillis,
            this.headers,
            this.sslConfiguration,
            this.verifyHostname
         );
         return new HttpAppender(this.getName(), this.getLayout(), this.getFilter(), this.isIgnoreExceptions(), httpManager, this.getPropertyArray());
      }

      public URL getUrl() {
         return this.url;
      }

      public String getMethod() {
         return this.method;
      }

      public int getConnectTimeoutMillis() {
         return this.connectTimeoutMillis;
      }

      public int getReadTimeoutMillis() {
         return this.readTimeoutMillis;
      }

      public Property[] getHeaders() {
         return this.headers;
      }

      public SslConfiguration getSslConfiguration() {
         return this.sslConfiguration;
      }

      public boolean isVerifyHostname() {
         return this.verifyHostname;
      }

      public B setUrl(final URL url) {
         this.url = url;
         return this.asBuilder();
      }

      public B setMethod(final String method) {
         this.method = method;
         return this.asBuilder();
      }

      public B setConnectTimeoutMillis(final int connectTimeoutMillis) {
         this.connectTimeoutMillis = connectTimeoutMillis;
         return this.asBuilder();
      }

      public B setReadTimeoutMillis(final int readTimeoutMillis) {
         this.readTimeoutMillis = readTimeoutMillis;
         return this.asBuilder();
      }

      public B setHeaders(final Property[] headers) {
         this.headers = headers;
         return this.asBuilder();
      }

      public B setSslConfiguration(final SslConfiguration sslConfiguration) {
         this.sslConfiguration = sslConfiguration;
         return this.asBuilder();
      }

      public B setVerifyHostname(final boolean verifyHostname) {
         this.verifyHostname = verifyHostname;
         return this.asBuilder();
      }
   }
}
