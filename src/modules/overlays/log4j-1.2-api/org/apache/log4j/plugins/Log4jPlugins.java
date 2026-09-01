package org.apache.log4j.plugins;

import org.apache.logging.log4j.plugins.model.PluginEntry;
import org.apache.logging.log4j.plugins.model.PluginService;

public class Log4jPlugins extends PluginService {
   private static final PluginEntry[] ENTRIES = new PluginEntry[]{
      PluginEntry.builder()
         .setKey("log4j1ndcpatternconverter")
         .setClassName("org.apache.log4j.pattern.Log4j1NdcPatternConverter")
         .setName("Log4j1NdcPatternConverter")
         .setNamespace("Converter")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.consoleappender")
         .setClassName("org.apache.log4j.builders.appender.ConsoleAppenderBuilder")
         .setName("org.apache.log4j.ConsoleAppender")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.dailyrollingfileappender")
         .setClassName("org.apache.log4j.builders.appender.DailyRollingFileAppenderBuilder")
         .setName("org.apache.log4j.DailyRollingFileAppender")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.varia.stringmatchfilter")
         .setClassName("org.apache.log4j.builders.filter.StringMatchFilterBuilder")
         .setName("org.apache.log4j.varia.StringMatchFilter")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("log4j1xmlconfigurationfactory")
         .setClassName("org.apache.log4j.xml.XmlConfigurationFactory")
         .setName("Log4j1XmlConfigurationFactory")
         .setNamespace("ConfigurationFactory")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.htmllayout")
         .setClassName("org.apache.log4j.builders.layout.HtmlLayoutBuilder")
         .setName("org.apache.log4j.HTMLLayout")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.net.syslogappender")
         .setClassName("org.apache.log4j.builders.appender.SyslogAppenderBuilder")
         .setName("org.apache.log4j.net.SyslogAppender")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("log4j1sysloglayout")
         .setClassName("org.apache.log4j.layout.Log4j1SyslogLayout")
         .setName("Log4j1SyslogLayout")
         .setNamespace("Core")
         .setElementType("layout")
         .setPrintable(true)
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.net.socketappender")
         .setClassName("org.apache.log4j.builders.appender.SocketAppenderBuilder")
         .setName("org.apache.log4j.net.SocketAppender")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.xml.xmllayout")
         .setClassName("org.apache.log4j.builders.layout.XmlLayoutBuilder")
         .setName("org.apache.log4j.xml.XMLLayout")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.simplelayout")
         .setClassName("org.apache.log4j.builders.layout.SimpleLayoutBuilder")
         .setName("org.apache.log4j.SimpleLayout")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("log4j1propertiesconfigurationfactory")
         .setClassName("org.apache.log4j.config.PropertiesConfigurationFactory")
         .setName("Log4j1PropertiesConfigurationFactory")
         .setNamespace("ConfigurationFactory")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.rollingfileappender")
         .setClassName("org.apache.log4j.builders.appender.RollingFileAppenderBuilder")
         .setName("org.apache.log4j.RollingFileAppender")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.varia.levelrangefilter")
         .setClassName("org.apache.log4j.builders.filter.LevelRangeFilterBuilder")
         .setName("org.apache.log4j.varia.LevelRangeFilter")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("log4j1xmllayout")
         .setClassName("org.apache.log4j.layout.Log4j1XmlLayout")
         .setName("Log4j1XmlLayout")
         .setNamespace("Core")
         .setElementType("layout")
         .setPrintable(true)
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.fileappender")
         .setClassName("org.apache.log4j.builders.appender.FileAppenderBuilder")
         .setName("org.apache.log4j.FileAppender")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.varia.nullappender")
         .setClassName("org.apache.log4j.builders.appender.NullAppenderBuilder")
         .setName("org.apache.log4j.varia.NullAppender")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("log4j1levelpatternconverter")
         .setClassName("org.apache.log4j.pattern.Log4j1LevelPatternConverter")
         .setName("Log4j1LevelPatternConverter")
         .setNamespace("Converter")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.asyncappender")
         .setClassName("org.apache.log4j.builders.appender.AsyncAppenderBuilder")
         .setName("org.apache.log4j.AsyncAppender")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.rewrite.rewriteappender")
         .setClassName("org.apache.log4j.builders.appender.RewriteAppenderBuilder")
         .setName("org.apache.log4j.rewrite.RewriteAppender")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.varia.denyallfilter")
         .setClassName("org.apache.log4j.builders.filter.DenyAllFilterBuilder")
         .setName("org.apache.log4j.varia.DenyAllFilter")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.ttcclayout")
         .setClassName("org.apache.log4j.builders.layout.TTCCLayoutBuilder")
         .setName("org.apache.log4j.TTCCLayout")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("log4j1mdcpatternconverter")
         .setClassName("org.apache.log4j.pattern.Log4j1MdcPatternConverter")
         .setName("Log4j1MdcPatternConverter")
         .setNamespace("Converter")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.patternlayout")
         .setClassName("org.apache.log4j.builders.layout.PatternLayoutBuilder")
         .setName("org.apache.log4j.PatternLayout")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.enhancedpatternlayout")
         .setClassName("org.apache.log4j.builders.layout.PatternLayoutBuilder")
         .setName("org.apache.log4j.PatternLayout")
         .setNamespace("Log4j Builder")
         .get(),
      PluginEntry.builder()
         .setKey("org.apache.log4j.varia.levelmatchfilter")
         .setClassName("org.apache.log4j.builders.filter.LevelMatchFilterBuilder")
         .setName("org.apache.log4j.varia.LevelMatchFilter")
         .setNamespace("Log4j Builder")
         .get()
   };

   public PluginEntry[] getEntries() {
      return ENTRIES;
   }
}
