package org.apache.logging.log4j.core.layout;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.pattern.DatePatternConverter;
import org.apache.logging.log4j.core.util.Transform;
import org.apache.logging.log4j.plugins.Configurable;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.PluginBuilderAttribute;
import org.apache.logging.log4j.plugins.PluginFactory;
import org.apache.logging.log4j.util.Strings;

@Configurable(elementType = "layout", printObject = true)
@Plugin
public final class HtmlLayout extends AbstractStringLayout {
   public static final String DEFAULT_FONT_FAMILY = "arial,sans-serif";
   private static final String TRACE_PREFIX = "<br />&nbsp;&nbsp;&nbsp;&nbsp;";
   private static final String REGEXP = Strings.LINE_SEPARATOR.equals("\n") ? "\n" : Strings.LINE_SEPARATOR + "|\n";
   private static final String DEFAULT_TITLE = "Log4j Log Messages";
   private static final String DEFAULT_CONTENT_TYPE = "text/html";
   private static final String DEFAULT_DATE_PATTERN = "JVM_ELAPSE_TIME";
   private final long jvmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
   private final boolean locationInfo;
   private final String title;
   private final String contentType;
   private final String font;
   private final String fontSize;
   private final String headerSize;
   private final DatePatternConverter datePatternConverter;

   private HtmlLayout(
      final boolean locationInfo,
      final String title,
      final String contentType,
      final Charset charset,
      final String font,
      final String fontSize,
      final String headerSize,
      final String datePattern,
      final String timezone
   ) {
      super(charset);
      this.locationInfo = locationInfo;
      this.title = title;
      this.contentType = this.addCharsetToContentType(contentType);
      this.font = font;
      this.fontSize = fontSize;
      this.headerSize = headerSize;
      this.datePatternConverter = "JVM_ELAPSE_TIME".equals(datePattern) ? null : DatePatternConverter.newInstance(new String[]{datePattern, timezone});
   }

   public String getTitle() {
      return this.title;
   }

   public boolean isLocationInfo() {
      return this.locationInfo;
   }

   @Override
   public boolean requiresLocation() {
      return this.locationInfo;
   }

   private String addCharsetToContentType(final String contentType) {
      if (contentType == null) {
         return "text/html; charset=" + this.getCharset();
      } else {
         return contentType.contains("charset") ? contentType : contentType + "; charset=" + this.getCharset();
      }
   }

   @Override
   public String toSerializable(final LogEvent event) {
      StringBuilder sbuf = getStringBuilder();
      sbuf.append(Strings.LINE_SEPARATOR).append("<tr>").append(Strings.LINE_SEPARATOR);
      sbuf.append("<td>");
      if (this.datePatternConverter == null) {
         sbuf.append(event.getTimeMillis() - this.jvmStartTime);
      } else {
         this.datePatternConverter.format(event, sbuf);
      }

      sbuf.append("</td>").append(Strings.LINE_SEPARATOR);
      String escapedThread = Transform.escapeHtmlTags(event.getThreadName());
      sbuf.append("<td title=\"").append(escapedThread).append(" thread\">");
      sbuf.append(escapedThread);
      sbuf.append("</td>").append(Strings.LINE_SEPARATOR);
      sbuf.append("<td title=\"Level\">");
      if (event.getLevel().equals(Level.DEBUG)) {
         sbuf.append("<font color=\"#339933\">");
         sbuf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
         sbuf.append("</font>");
      } else if (event.getLevel().isMoreSpecificThan(Level.WARN)) {
         sbuf.append("<font color=\"#993300\"><strong>");
         sbuf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
         sbuf.append("</strong></font>");
      } else {
         sbuf.append(Transform.escapeHtmlTags(String.valueOf(event.getLevel())));
      }

      sbuf.append("</td>").append(Strings.LINE_SEPARATOR);
      String escapedLogger = Transform.escapeHtmlTags(event.getLoggerName());
      if (Strings.isEmpty(escapedLogger)) {
         escapedLogger = "root";
      }

      sbuf.append("<td title=\"").append(escapedLogger).append(" logger\">");
      sbuf.append(escapedLogger);
      sbuf.append("</td>").append(Strings.LINE_SEPARATOR);
      if (this.locationInfo) {
         StackTraceElement element = event.getSource();
         sbuf.append("<td>");
         sbuf.append(Transform.escapeHtmlTags(element.getFileName()));
         sbuf.append(':');
         sbuf.append(element.getLineNumber());
         sbuf.append("</td>").append(Strings.LINE_SEPARATOR);
      }

      sbuf.append("<td title=\"Message\">");
      sbuf.append(Transform.escapeHtmlTags(event.getMessage().getFormattedMessage()).replaceAll(REGEXP, "<br />"));
      sbuf.append("</td>").append(Strings.LINE_SEPARATOR);
      sbuf.append("</tr>").append(Strings.LINE_SEPARATOR);
      if (event.getContextStack() != null && !event.getContextStack().isEmpty()) {
         sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : ").append(this.fontSize);
         sbuf.append(";\" colspan=\"6\" ");
         sbuf.append("title=\"Nested Diagnostic Context\">");
         sbuf.append("NDC: ").append(Transform.escapeHtmlTags(event.getContextStack().toString()));
         sbuf.append("</td></tr>").append(Strings.LINE_SEPARATOR);
      }

      if (event.getContextData() != null && !event.getContextData().isEmpty()) {
         sbuf.append("<tr><td bgcolor=\"#EEEEEE\" style=\"font-size : ").append(this.fontSize);
         sbuf.append(";\" colspan=\"6\" ");
         sbuf.append("title=\"Mapped Diagnostic Context\">");
         sbuf.append("MDC: ").append(Transform.escapeHtmlTags(event.getContextData().toMap().toString()));
         sbuf.append("</td></tr>").append(Strings.LINE_SEPARATOR);
      }

      Throwable throwable = event.getThrown();
      if (throwable != null) {
         sbuf.append("<tr><td bgcolor=\"#993300\" style=\"color:White; font-size : ").append(this.fontSize);
         sbuf.append(";\" colspan=\"6\">");
         this.appendThrowableAsHtml(throwable, sbuf);
         sbuf.append("</td></tr>").append(Strings.LINE_SEPARATOR);
      }

      return sbuf.toString();
   }

   @Override
   public String getContentType() {
      return this.contentType;
   }

   private void appendThrowableAsHtml(final Throwable throwable, final StringBuilder sbuf) {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);

      try {
         throwable.printStackTrace(pw);
      } catch (RuntimeException var10) {
      }

      pw.flush();
      LineNumberReader reader = new LineNumberReader(new StringReader(sw.toString()));
      ArrayList<String> lines = new ArrayList<>();

      try {
         for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            lines.add(line);
         }
      } catch (IOException ex) {
         if (ex instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         lines.add(ex.toString());
      }

      boolean first = true;

      for (String line : lines) {
         if (!first) {
            sbuf.append("<br />&nbsp;&nbsp;&nbsp;&nbsp;");
         } else {
            first = false;
         }

         sbuf.append(Transform.escapeHtmlTags(line));
         sbuf.append(Strings.LINE_SEPARATOR);
      }
   }

   private StringBuilder appendLs(final StringBuilder sbuilder, final String s) {
      sbuilder.append(s).append(Strings.LINE_SEPARATOR);
      return sbuilder;
   }

   private StringBuilder append(final StringBuilder sbuilder, final String s) {
      sbuilder.append(s);
      return sbuilder;
   }

   @Override
   public byte[] getHeader() {
      StringBuilder sbuf = new StringBuilder();
      this.append(sbuf, "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" ");
      this.appendLs(sbuf, "\"http://www.w3.org/TR/html4/loose.dtd\">");
      this.appendLs(sbuf, "<html>");
      this.appendLs(sbuf, "<head>");
      this.append(sbuf, "<meta charset=\"");
      this.append(sbuf, this.getCharset().toString());
      this.appendLs(sbuf, "\"/>");
      this.append(sbuf, "<title>").append(this.title);
      this.appendLs(sbuf, "</title>");
      this.appendLs(sbuf, "<style type=\"text/css\">");
      this.appendLs(sbuf, "<!--");
      this.append(sbuf, "body, table {font-family:").append(this.font).append("; font-size: ");
      this.appendLs(sbuf, this.headerSize).append(";}");
      this.appendLs(sbuf, "th {background: #336699; color: #FFFFFF; text-align: left;}");
      this.appendLs(sbuf, "-->");
      this.appendLs(sbuf, "</style>");
      this.appendLs(sbuf, "</head>");
      this.appendLs(sbuf, "<body bgcolor=\"#FFFFFF\" topmargin=\"6\" leftmargin=\"6\">");
      this.appendLs(sbuf, "<hr size=\"1\" noshade=\"noshade\">");
      this.appendLs(sbuf, "Log session start time " + new Date() + "<br>");
      this.appendLs(sbuf, "<br>");
      this.appendLs(sbuf, "<table cellspacing=\"0\" cellpadding=\"4\" border=\"1\" bordercolor=\"#224466\" width=\"100%\">");
      this.appendLs(sbuf, "<tr>");
      this.appendLs(sbuf, "<th>Time</th>");
      this.appendLs(sbuf, "<th>Thread</th>");
      this.appendLs(sbuf, "<th>Level</th>");
      this.appendLs(sbuf, "<th>Logger</th>");
      if (this.locationInfo) {
         this.appendLs(sbuf, "<th>File:Line</th>");
      }

      this.appendLs(sbuf, "<th>Message</th>");
      this.appendLs(sbuf, "</tr>");
      return sbuf.toString().getBytes(this.getCharset());
   }

   @Override
   public byte[] getFooter() {
      StringBuilder sbuf = new StringBuilder();
      this.appendLs(sbuf, "</table>");
      this.appendLs(sbuf, "<br>");
      this.appendLs(sbuf, "</body></html>");
      return this.getBytes(sbuf.toString());
   }

   public static HtmlLayout createDefaultLayout() {
      return newBuilder().build();
   }

   @PluginFactory
   public static HtmlLayout.Builder newBuilder() {
      return new HtmlLayout.Builder();
   }

   public static final class Builder implements org.apache.logging.log4j.plugins.util.Builder<HtmlLayout> {
      @PluginBuilderAttribute
      private boolean locationInfo = false;
      @PluginBuilderAttribute
      private String title = "Log4j Log Messages";
      @PluginBuilderAttribute
      private String contentType = null;
      @PluginBuilderAttribute
      private Charset charset = StandardCharsets.UTF_8;
      @PluginBuilderAttribute
      private HtmlLayout.FontSize fontSize = HtmlLayout.FontSize.SMALL;
      @PluginBuilderAttribute
      private String fontName = "arial,sans-serif";
      @PluginBuilderAttribute
      private String datePattern = "JVM_ELAPSE_TIME";
      @PluginBuilderAttribute
      private String timezone = null;

      private Builder() {
      }

      public HtmlLayout.Builder setLocationInfo(final boolean locationInfo) {
         this.locationInfo = locationInfo;
         return this;
      }

      public HtmlLayout.Builder setTitle(final String title) {
         this.title = title;
         return this;
      }

      public HtmlLayout.Builder setContentType(final String contentType) {
         this.contentType = contentType;
         return this;
      }

      public HtmlLayout.Builder setCharset(final Charset charset) {
         this.charset = charset;
         return this;
      }

      public HtmlLayout.Builder setFontSize(final HtmlLayout.FontSize fontSize) {
         this.fontSize = fontSize;
         return this;
      }

      public HtmlLayout.Builder setFontName(final String fontName) {
         this.fontName = fontName;
         return this;
      }

      public HtmlLayout.Builder setDatePattern(final String datePattern) {
         this.datePattern = datePattern;
         return this;
      }

      public HtmlLayout.Builder setTimezone(final String timezone) {
         this.timezone = timezone;
         return this;
      }

      public HtmlLayout build() {
         if (this.contentType == null) {
            this.contentType = "text/html; charset=" + this.charset;
         }

         return new HtmlLayout(
            this.locationInfo,
            this.title,
            this.contentType,
            this.charset,
            this.fontName,
            this.fontSize.getFontSize(),
            this.fontSize.larger().getFontSize(),
            this.datePattern,
            this.timezone
         );
      }
   }

   public enum FontSize {
      SMALLER("smaller"),
      XXSMALL("xx-small"),
      XSMALL("x-small"),
      SMALL("small"),
      MEDIUM("medium"),
      LARGE("large"),
      XLARGE("x-large"),
      XXLARGE("xx-large"),
      LARGER("larger");

      private final String size;

      FontSize(final String size) {
         this.size = size;
      }

      public String getFontSize() {
         return this.size;
      }

      public static HtmlLayout.FontSize getFontSize(final String size) {
         for (HtmlLayout.FontSize fontSize : values()) {
            if (fontSize.size.equals(size)) {
               return fontSize;
            }
         }

         return SMALL;
      }

      public HtmlLayout.FontSize larger() {
         return this.ordinal() < XXLARGE.ordinal() ? values()[this.ordinal() + 1] : this;
      }
   }
}
