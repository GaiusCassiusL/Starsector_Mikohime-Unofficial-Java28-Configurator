package org.apache.logging.log4j.core.layout;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.net.Facility;
import org.apache.logging.log4j.core.net.Priority;
import org.apache.logging.log4j.core.util.NetUtils;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;

@Configurable(elementType = "layout", printObject = true)
@Plugin
public final class SyslogLayout extends AbstractStringLayout {
   public static final Pattern NEWLINE_PATTERN = Pattern.compile("\\r?\\n");
   private final Facility facility;
   private final boolean includeNewLine;
   private final String escapeNewLine;
   private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss", Locale.ENGLISH);
   private final String localHostname = NetUtils.getLocalHostname();

   @PluginFactory
   public static <B extends SyslogLayout.Builder<B>> B newBuilder() {
      return new SyslogLayout.Builder<B>().asBuilder();
   }

   protected SyslogLayout(final Facility facility, final boolean includeNL, final String escapeNL, final Charset charset) {
      super(charset);
      this.facility = facility;
      this.includeNewLine = includeNL;
      this.escapeNewLine = escapeNL == null ? null : Matcher.quoteReplacement(escapeNL);
   }

   @Override
   public String toSerializable(final LogEvent event) {
      StringBuilder buf = getStringBuilder();
      buf.append('<');
      buf.append(Priority.getPriority(this.facility, event.getLevel()));
      buf.append('>');
      this.addDate(event.getTimeMillis(), buf);
      buf.append(' ');
      buf.append(this.localHostname);
      buf.append(' ');
      String message = event.getMessage().getFormattedMessage();
      if (null != this.escapeNewLine) {
         message = NEWLINE_PATTERN.matcher(message).replaceAll(this.escapeNewLine);
      }

      buf.append(message);
      if (this.includeNewLine) {
         buf.append('\n');
      }

      return buf.toString();
   }

   private synchronized void addDate(final long timestamp, final StringBuilder buf) {
      int index = buf.length() + 4;
      buf.append(this.dateFormat.format(new Date(timestamp)));
      if (buf.charAt(index) == '0') {
         buf.setCharAt(index, ' ');
      }
   }

   @Override
   public Map<String, String> getContentFormat() {
      Map<String, String> result = new HashMap<>();
      result.put("structured", "false");
      result.put("formatType", "logfilepatternreceiver");
      result.put("dateFormat", this.dateFormat.toPattern());
      result.put("format", "<LEVEL>TIMESTAMP PROP(HOSTNAME) MESSAGE");
      return result;
   }

   public Facility getFacility() {
      return this.facility;
   }

   public static class Builder<B extends SyslogLayout.Builder<B>>
      extends AbstractStringLayout.Builder<B>
      implements org.apache.logging.log4j.plugins.util.Builder<SyslogLayout> {
      @PluginBuilderAttribute
      private Facility facility = Facility.LOCAL0;
      @PluginBuilderAttribute("newLine")
      private boolean includeNewLine;
      @PluginBuilderAttribute("newLineEscape")
      private String escapeNL;

      public Builder() {
         this.setCharset(StandardCharsets.UTF_8);
      }

      public SyslogLayout build() {
         return new SyslogLayout(this.facility, this.includeNewLine, this.escapeNL, this.getCharset());
      }

      public Facility getFacility() {
         return this.facility;
      }

      public boolean isIncludeNewLine() {
         return this.includeNewLine;
      }

      public String getEscapeNL() {
         return this.escapeNL;
      }

      public B setFacility(final Facility facility) {
         this.facility = facility;
         return this.asBuilder();
      }

      public B setIncludeNewLine(final boolean includeNewLine) {
         this.includeNewLine = includeNewLine;
         return this.asBuilder();
      }

      public B setEscapeNL(final String escapeNL) {
         this.escapeNL = escapeNL;
         return this.asBuilder();
      }
   }
}
