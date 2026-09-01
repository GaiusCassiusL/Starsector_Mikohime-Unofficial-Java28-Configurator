package org.apache.logging.log4j.core.appender;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.LoggerFields;
import org.apache.logging.log4j.core.layout.Rfc5424Layout;
import org.apache.logging.log4j.core.layout.SyslogLayout;
import org.apache.logging.log4j.core.net.AbstractSocketManager;
import org.apache.logging.log4j.core.net.Advertiser;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Protocol;
import org.apache.logging.log4j.core.net.ssl.SslConfiguration;
import org.apache.logging.log4j.core.util.Constants;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "appender", printObject = true)
@Plugin("Syslog")
public class SyslogAppender extends SocketAppender {
   protected static final String RFC5424 = "RFC5424";

   protected SyslogAppender(
      final String name,
      final Layout layout,
      final Filter filter,
      final boolean ignoreExceptions,
      final boolean immediateFlush,
      final AbstractSocketManager manager,
      final Advertiser advertiser
   ) {
      super(name, layout, filter, manager, ignoreExceptions, immediateFlush, advertiser);
   }

   @PluginFactory
   public static <B extends SyslogAppender.Builder<B>> B newSyslogAppenderBuilder() {
      return new SyslogAppender.Builder<B>().asBuilder();
   }

   public static class Builder<B extends SyslogAppender.Builder<B>>
      extends SocketAppender.AbstractBuilder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<SocketAppender> {
      @PluginBuilderAttribute("facility")
      private Facility facility = Facility.LOCAL0;
      @PluginBuilderAttribute("id")
      private String id;
      @PluginBuilderAttribute("enterpriseNumber")
      private String enterpriseNumber = String.valueOf(32473);
      @PluginBuilderAttribute("includeMdc")
      private boolean includeMdc = true;
      @PluginBuilderAttribute("mdcId")
      private String mdcId;
      @PluginBuilderAttribute("mdcPrefix")
      private String mdcPrefix;
      @PluginBuilderAttribute("eventPrefix")
      private String eventPrefix;
      @PluginBuilderAttribute("newLine")
      private boolean newLine;
      @PluginBuilderAttribute("newLineEscape")
      private String escapeNL;
      @PluginBuilderAttribute("appName")
      private String appName;
      @PluginBuilderAttribute("messageId")
      private String msgId;
      @PluginBuilderAttribute("mdcExcludes")
      private String excludes;
      @PluginBuilderAttribute("mdcIncludes")
      private String includes;
      @PluginBuilderAttribute("mdcRequired")
      private String required;
      @PluginBuilderAttribute("format")
      private String format;
      @PluginBuilderAttribute("charset")
      private Charset charsetName = StandardCharsets.UTF_8;
      @PluginBuilderAttribute("exceptionPattern")
      private String exceptionPattern;
      @PluginElement("LoggerFields")
      private LoggerFields[] loggerFields;

      public SyslogAppender build() {
         Protocol protocol = this.getProtocol();
         SslConfiguration sslConfiguration = this.getSslConfiguration();
         boolean useTlsMessageFormat = sslConfiguration != null || protocol == Protocol.SSL;
         Configuration configuration = this.getConfiguration();
         Layout layout = this.getLayout();
         if (layout == null) {
            layout = "RFC5424".equalsIgnoreCase(this.format)
               ? new Rfc5424Layout.Rfc5424LayoutBuilder()
                  .setFacility(this.facility)
                  .setId(this.id)
                  .setEin(this.enterpriseNumber)
                  .setIncludeMDC(this.includeMdc)
                  .setMdcId(this.mdcId)
                  .setMdcPrefix(this.mdcPrefix)
                  .setEventPrefix(this.eventPrefix)
                  .setIncludeNL(this.newLine)
                  .setEscapeNL(this.escapeNL)
                  .setAppName(this.appName)
                  .setMessageId(this.msgId)
                  .setExcludes(this.excludes)
                  .setIncludes(this.includes)
                  .setRequired(this.required)
                  .setExceptionPattern(this.exceptionPattern)
                  .setUseTLSMessageFormat(useTlsMessageFormat)
                  .setLoggerFields(this.loggerFields)
                  .setConfig(configuration)
                  .build()
               : ((SyslogLayout.Builder)SyslogLayout.newBuilder()
                     .setFacility(this.facility)
                     .setIncludeNewLine(this.newLine)
                     .setEscapeNL(this.escapeNL)
                     .setCharset(this.charsetName))
                  .build();
         }

         String name = this.getName();
         if (name == null) {
            SyslogAppender.LOGGER.error("No name provided for SyslogAppender");
            return null;
         } else {
            AbstractSocketManager manager = SocketAppender.createSocketManager(
               name,
               protocol,
               this.getHost(),
               this.getPort(),
               this.getConnectTimeoutMillis(),
               sslConfiguration,
               this.getReconnectDelayMillis(),
               this.getImmediateFail(),
               layout,
               Constants.ENCODER_BYTE_BUFFER_SIZE,
               null
            );
            return new SyslogAppender(
               name,
               layout,
               this.getFilter(),
               this.isIgnoreExceptions(),
               this.isImmediateFlush(),
               manager,
               this.getAdvertise() ? configuration.getAdvertiser() : null
            );
         }
      }

      public Facility getFacility() {
         return this.facility;
      }

      public String getId() {
         return this.id;
      }

      public String getEnterpriseNumber() {
         return this.enterpriseNumber;
      }

      public boolean isIncludeMdc() {
         return this.includeMdc;
      }

      public String getMdcId() {
         return this.mdcId;
      }

      public String getMdcPrefix() {
         return this.mdcPrefix;
      }

      public String getEventPrefix() {
         return this.eventPrefix;
      }

      public boolean isNewLine() {
         return this.newLine;
      }

      public String getEscapeNL() {
         return this.escapeNL;
      }

      public String getAppName() {
         return this.appName;
      }

      public String getMsgId() {
         return this.msgId;
      }

      public String getExcludes() {
         return this.excludes;
      }

      public String getIncludes() {
         return this.includes;
      }

      public String getRequired() {
         return this.required;
      }

      public String getFormat() {
         return this.format;
      }

      public Charset getCharsetName() {
         return this.charsetName;
      }

      public String getExceptionPattern() {
         return this.exceptionPattern;
      }

      public LoggerFields[] getLoggerFields() {
         return this.loggerFields;
      }

      public B setFacility(final Facility facility) {
         this.facility = facility;
         return this.asBuilder();
      }

      public B setId(final String id) {
         this.id = id;
         return this.asBuilder();
      }

      public B setEnterpriseNumber(final String enterpriseNumber) {
         this.enterpriseNumber = enterpriseNumber;
         return this.asBuilder();
      }

      /** @deprecated */
      public B setEnterpriseNumber(final int enterpriseNumber) {
         this.enterpriseNumber = String.valueOf(enterpriseNumber);
         return this.asBuilder();
      }

      public B setIncludeMdc(final boolean includeMdc) {
         this.includeMdc = includeMdc;
         return this.asBuilder();
      }

      public B setMdcId(final String mdcId) {
         this.mdcId = mdcId;
         return this.asBuilder();
      }

      public B setMdcPrefix(final String mdcPrefix) {
         this.mdcPrefix = mdcPrefix;
         return this.asBuilder();
      }

      public B setEventPrefix(final String eventPrefix) {
         this.eventPrefix = eventPrefix;
         return this.asBuilder();
      }

      public B setNewLine(final boolean newLine) {
         this.newLine = newLine;
         return this.asBuilder();
      }

      public B setEscapeNL(final String escapeNL) {
         this.escapeNL = escapeNL;
         return this.asBuilder();
      }

      public B setAppName(final String appName) {
         this.appName = appName;
         return this.asBuilder();
      }

      public B setMsgId(final String msgId) {
         this.msgId = msgId;
         return this.asBuilder();
      }

      public B setExcludes(final String excludes) {
         this.excludes = excludes;
         return this.asBuilder();
      }

      public B setIncludes(final String includes) {
         this.includes = includes;
         return this.asBuilder();
      }

      public B setRequired(final String required) {
         this.required = required;
         return this.asBuilder();
      }

      public B setFormat(final String format) {
         this.format = format;
         return this.asBuilder();
      }

      public B setCharsetName(final Charset charset) {
         this.charsetName = charset;
         return this.asBuilder();
      }

      public B setExceptionPattern(final String exceptionPattern) {
         this.exceptionPattern = exceptionPattern;
         return this.asBuilder();
      }

      public B setLoggerFields(final LoggerFields[] loggerFields) {
         this.loggerFields = loggerFields;
         return this.asBuilder();
      }
   }
}
