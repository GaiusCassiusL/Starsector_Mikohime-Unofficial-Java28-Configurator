package org.apache.log4j.layout;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.StringLayout;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.layout.AbstractStringLayout;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.pattern.DatePatternConverter;
import org.apache.logging.log4j.core.pattern.LogEventPatternConverter;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginElement;

@Configurable(elementType = "layout", printObject = true)
@Plugin
public final class Log4j1SyslogLayout extends AbstractStringLayout {
   private static final String localHostname = NetUtils.getLocalHostname();
   private final Facility facility;
   private final boolean facilityPrinting;
   private final boolean header;
   private final StringLayout messageLayout;
   private static final String[] dateFormatOptions = new String[]{"MMM dd HH:mm:ss", null, "en"};
   private final LogEventPatternConverter dateConverter = DatePatternConverter.newInstance(dateFormatOptions);

   @PluginBuilderFactory
   public static <B extends Log4j1SyslogLayout.Builder<B>> B newBuilder() {
      return (B)new Log4j1SyslogLayout.Builder().asBuilder();
   }

   private Log4j1SyslogLayout(
      final Facility facility, final boolean facilityPrinting, final boolean header, final StringLayout messageLayout, final Charset charset
   ) {
      super(charset);
      this.facility = facility;
      this.facilityPrinting = facilityPrinting;
      this.header = header;
      this.messageLayout = messageLayout;
   }

   public String toSerializable(final LogEvent event) {
      String message = this.messageLayout != null ? this.messageLayout.toSerializable(event) : event.getMessage().getFormattedMessage();
      StringBuilder buf = getStringBuilder();
      buf.append('<');
      buf.append(Priority.getPriority(this.facility, event.getLevel()));
      buf.append('>');
      if (this.header) {
         int index = buf.length() + 4;
         this.dateConverter.format(event, buf);
         if (buf.charAt(index) == '0') {
            buf.setCharAt(index, ' ');
         }

         buf.append(' ');
         buf.append(localHostname);
         buf.append(' ');
      }

      if (this.facilityPrinting) {
         buf.append(this.facility != null ? this.facility.name().toLowerCase() : "user").append(':');
      }

      buf.append(message);
      return buf.toString();
   }

   public Map<String, String> getContentFormat() {
      Map<String, String> result = new HashMap<>();
      result.put("structured", "false");
      result.put("formatType", "logfilepatternreceiver");
      result.put("dateFormat", dateFormatOptions[0]);
      if (this.header) {
         result.put("format", "<LEVEL>TIMESTAMP PROP(HOSTNAME) MESSAGE");
      } else {
         result.put("format", "<LEVEL>MESSAGE");
      }

      return result;
   }

   public static class Builder<B extends Log4j1SyslogLayout.Builder<B>>
      extends org.apache.logging.log4j.core.layout.AbstractStringLayout.Builder<B>
      implements org.apache.logging.log4j.core.util.Builder<Log4j1SyslogLayout> {
      @PluginBuilderAttribute
      private Facility facility = Facility.USER;
      @PluginBuilderAttribute
      private boolean facilityPrinting;
      @PluginBuilderAttribute
      private boolean header;
      @PluginElement("Layout")
      private Layout messageLayout;

      public Builder() {
         this.setCharset(StandardCharsets.UTF_8);
      }

      public Log4j1SyslogLayout build() {
         if (this.messageLayout != null && !(this.messageLayout instanceof StringLayout)) {
            Log4j1SyslogLayout.LOGGER.error("Log4j1SyslogLayout: the message layout must be a StringLayout.");
            return null;
         } else {
            return new Log4j1SyslogLayout(this.facility, this.facilityPrinting, this.header, (StringLayout)this.messageLayout, this.getCharset());
         }
      }

      public Facility getFacility() {
         return this.facility;
      }

      public boolean isFacilityPrinting() {
         return this.facilityPrinting;
      }

      public boolean isHeader() {
         return this.header;
      }

      public Layout getMessageLayout() {
         return this.messageLayout;
      }

      public B setFacility(final Facility facility) {
         this.facility = facility;
         return (B)this.asBuilder();
      }

      public B setFacilityPrinting(final boolean facilityPrinting) {
         this.facilityPrinting = facilityPrinting;
         return (B)this.asBuilder();
      }

      public B setHeader(final boolean header) {
         this.header = header;
         return (B)this.asBuilder();
      }

      public B setMessageLayout(final Layout messageLayout) {
         this.messageLayout = messageLayout;
         return (B)this.asBuilder();
      }
   }
}
