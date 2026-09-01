package org.apache.log4j.xml;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.bridge.AppenderAdapter;
import org.apache.log4j.bridge.FilterAdapter;
import org.apache.log4j.config.Log4j1Configuration;
import org.apache.log4j.config.PropertySetter;
import org.apache.log4j.helpers.OptionConverter;
import org.apache.log4j.rewrite.RewritePolicy;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LifeCycle.State;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.status.StatusConfiguration;
import org.apache.logging.log4j.core.filter.ThresholdFilter;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.util.LoaderUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class XmlConfiguration extends Log4j1Configuration {
   private static final Logger LOGGER = StatusLogger.getLogger();
   private static final String CONFIGURATION_TAG = "log4j:configuration";
   private static final String OLD_CONFIGURATION_TAG = "configuration";
   private static final String RENDERER_TAG = "renderer";
   private static final String APPENDER_TAG = "appender";
   public static final String PARAM_TAG = "param";
   public static final String LAYOUT_TAG = "layout";
   private static final String CATEGORY = "category";
   private static final String LOGGER_ELEMENT = "logger";
   private static final String CATEGORY_FACTORY_TAG = "categoryFactory";
   private static final String LOGGER_FACTORY_TAG = "loggerFactory";
   public static final String NAME_ATTR = "name";
   private static final String CLASS_ATTR = "class";
   public static final String VALUE_ATTR = "value";
   private static final String ROOT_TAG = "root";
   private static final String LEVEL_TAG = "level";
   private static final String PRIORITY_TAG = "priority";
   public static final String FILTER_TAG = "filter";
   private static final String ERROR_HANDLER_TAG = "errorHandler";
   public static final String REF_ATTR = "ref";
   private static final String ADDITIVITY_ATTR = "additivity";
   private static final String CONFIG_DEBUG_ATTR = "configDebug";
   private static final String INTERNAL_DEBUG_ATTR = "debug";
   private static final String THRESHOLD_ATTR = "threshold";
   private static final String EMPTY_STR = "";
   private static final Class<?>[] ONE_STRING_PARAM = new Class[]{String.class};
   private static final String dbfKey = "javax.xml.parsers.DocumentBuilderFactory";
   private static final String THROWABLE_RENDERER_TAG = "throwableRenderer";
   public static final long DEFAULT_DELAY = 60000L;
   protected static final String TEST_PREFIX = "log4j-test";
   protected static final String DEFAULT_PREFIX = "log4j";
   private final Map<String, Appender> appenderMap;
   private final Properties props = null;

   public XmlConfiguration(final LoggerContext loggerContext, final ConfigurationSource source, final int monitorIntervalSeconds) {
      super(loggerContext, source, monitorIntervalSeconds);
      this.appenderMap = new HashMap<>();
   }

   public void addAppenderIfAbsent(Appender appender) {
      this.appenderMap.putIfAbsent(appender.getName(), appender);
   }

   public void doConfigure() throws FactoryConfigurationError {
      final ConfigurationSource source = this.getConfigurationSource();
      XmlConfiguration.ParseAction action = new XmlConfiguration.ParseAction() {
         @Override
         public Document parse(final DocumentBuilder parser) throws SAXException, IOException {
            InputSource inputSource = new InputSource(source.getInputStream());
            inputSource.setSystemId("dummy://log4j.dtd");
            return parser.parse(inputSource);
         }

         @Override
         public String toString() {
            return XmlConfiguration.this.getConfigurationSource().getLocation();
         }
      };
      this.doConfigure(action);
   }

   private void doConfigure(final XmlConfiguration.ParseAction action) throws FactoryConfigurationError {
      DocumentBuilderFactory dbf;
      try {
         LOGGER.debug("System property is : {}", OptionConverter.getSystemProperty("javax.xml.parsers.DocumentBuilderFactory", null));
         dbf = DocumentBuilderFactory.newInstance();
         LOGGER.debug("Standard DocumentBuilderFactory search succeeded.");
         LOGGER.debug("DocumentBuilderFactory is: " + dbf.getClass().getName());
      } catch (FactoryConfigurationError fce) {
         Exception e = fce.getException();
         LOGGER.debug("Could not instantiate a DocumentBuilderFactory.", e);
         throw fce;
      }

      try {
         dbf.setValidating(true);
         DocumentBuilder docBuilder = dbf.newDocumentBuilder();
         docBuilder.setErrorHandler(new XmlConfiguration.SAXErrorHandler());
         docBuilder.setEntityResolver(new Log4jEntityResolver());
         Document doc = action.parse(docBuilder);
         this.parse(doc.getDocumentElement());
      } catch (Exception e) {
         if (e instanceof InterruptedException || e instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.error("Could not parse " + action.toString() + ".", e);
      }
   }

   @Override
   public Configuration reconfigure() {
      try {
         ConfigurationSource source = this.getConfigurationSource().resetInputStream();
         if (source == null) {
            return null;
         }

         XmlConfigurationFactory factory = new XmlConfigurationFactory();
         XmlConfiguration config = (XmlConfiguration)factory.getConfiguration(this.getLoggerContext(), source);
         return config != null && config.getState() == State.INITIALIZING ? config : null;
      } catch (IOException ex) {
         LOGGER.error("Cannot locate file {}: {}", this.getConfigurationSource(), ex);
         return null;
      }
   }

   private void parseUnrecognizedElement(final Object instance, final Element element, final Properties props) throws Exception {
      boolean recognized = false;
      if (instance instanceof UnrecognizedElementHandler) {
         recognized = ((UnrecognizedElementHandler)instance).parseUnrecognizedElement(element, props);
      }

      if (!recognized) {
         LOGGER.warn("Unrecognized element {}", element.getNodeName());
      }
   }

   private void quietParseUnrecognizedElement(final Object instance, final Element element, final Properties props) {
      try {
         this.parseUnrecognizedElement(instance, element, props);
      } catch (Exception ex) {
         if (ex instanceof InterruptedException || ex instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.error("Error in extension content: ", ex);
      }
   }

   public String subst(final String value, final Properties props) {
      try {
         return OptionConverter.substVars(value, props);
      } catch (IllegalArgumentException e) {
         LOGGER.warn("Could not perform variable substitution.", e);
         return value;
      }
   }

   public void setParameter(final Element elem, final PropertySetter propSetter, final Properties props) {
      String name = this.subst(elem.getAttribute("name"), props);
      String value = elem.getAttribute("value");
      value = this.subst(OptionConverter.convertSpecialChars(value), props);
      propSetter.setProperty(name, value);
   }

   public Object parseElement(final Element element, final Properties props, final Class expectedClass) throws Exception {
      String clazz = this.subst(element.getAttribute("class"), props);
      Object instance = OptionConverter.instantiateByClassName(clazz, expectedClass, null);
      if (instance != null) {
         PropertySetter propSetter = new PropertySetter(instance);
         NodeList children = element.getChildNodes();
         int length = children.getLength();

         for (int loop = 0; loop < length; loop++) {
            Node currentNode = children.item(loop);
            if (currentNode.getNodeType() == 1) {
               Element currentElement = (Element)currentNode;
               String tagName = currentElement.getTagName();
               if (tagName.equals("param")) {
                  this.setParameter(currentElement, propSetter, props);
               } else {
                  this.parseUnrecognizedElement(instance, currentElement, props);
               }
            }
         }

         return instance;
      } else {
         return null;
      }
   }

   private Appender findAppenderByName(final Document doc, final String appenderName) {
      Appender appender = this.appenderMap.get(appenderName);
      if (appender != null) {
         return appender;
      }

      Element element = null;
      NodeList list = doc.getElementsByTagName("appender");

      for (int t = 0; t < list.getLength(); t++) {
         Node node = list.item(t);
         NamedNodeMap map = node.getAttributes();
         Node attrNode = map.getNamedItem("name");
         if (appenderName.equals(attrNode.getNodeValue())) {
            element = (Element)node;
            break;
         }
      }

      if (element == null) {
         LOGGER.error("No appender named [{}] could be found.", appenderName);
         return null;
      }

      appender = this.parseAppender(element);
      if (appender != null) {
         this.appenderMap.put(appenderName, appender);
      }

      return appender;
   }

   public Appender findAppenderByReference(final Element appenderRef) {
      String appenderName = this.subst(appenderRef.getAttribute("ref"));
      Document doc = appenderRef.getOwnerDocument();
      return this.findAppenderByName(doc, appenderName);
   }

   public Appender parseAppender(final Element appenderElement) {
      String className = this.subst(appenderElement.getAttribute("class"));
      LOGGER.debug("Class name: [" + className + "]");
      Appender appender = this.manager.parseAppender(className, appenderElement, this);
      if (appender == null) {
         appender = this.buildAppender(className, appenderElement);
      }

      return appender;
   }

   private Appender buildAppender(final String className, final Element appenderElement) {
      try {
         Appender appender = (Appender)LoaderUtil.newInstanceOf(className);
         PropertySetter propSetter = new PropertySetter(appender);
         appender.setName(this.subst(appenderElement.getAttribute("name")));
         AtomicReference<Filter> filterChain = new AtomicReference<>();
         forEachElement(
            appenderElement.getChildNodes(),
            currentElement -> {
               switch (currentElement.getTagName()) {
                  case "param":
                     this.setParameter(currentElement, propSetter);
                     break;
                  case "layout":
                     appender.setLayout(this.parseLayout(currentElement));
                     break;
                  case "filter":
                     this.addFilter(filterChain, currentElement);
                     break;
                  case "errorHandler":
                     this.parseErrorHandler(currentElement, appender);
                     break;
                  case "appender-ref":
                     String refName = this.subst(currentElement.getAttribute("ref"));
                     if (appender instanceof AppenderAttachable) {
                        AppenderAttachable aa = (AppenderAttachable)appender;
                        Appender child = this.findAppenderByReference(currentElement);
                        LOGGER.debug("Attaching appender named [{}] to appender named [{}].", refName, appender.getName());
                        aa.addAppender(child);
                     } else {
                        LOGGER.error(
                           "Requesting attachment of appender named [{}] to appender named [{}]which does not implement org.apache.log4j.spi.AppenderAttachable.",
                           refName,
                           appender.getName()
                        );
                     }
                     break;
                  default:
                     try {
                        this.parseUnrecognizedElement(appender, currentElement, this.props);
                     } catch (Exception ex) {
                        throw new XmlConfiguration.ConsumerException(ex);
                     }
               }
            }
         );
         Filter head = filterChain.get();
         if (head != null) {
            appender.addFilter(head);
         }

         propSetter.activate();
         return appender;
      } catch (XmlConfiguration.ConsumerException ex) {
         Throwable t = ex.getCause();
         if (t instanceof InterruptedException || t instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.error("Could not create an Appender. Reported error follows.", t);
      } catch (Exception oops) {
         if (oops instanceof InterruptedException || oops instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.error("Could not create an Appender. Reported error follows.", oops);
      }

      return null;
   }

   public RewritePolicy parseRewritePolicy(final Element rewritePolicyElement) {
      String className = this.subst(rewritePolicyElement.getAttribute("class"));
      LOGGER.debug("Class name: [" + className + "]");
      RewritePolicy policy = this.manager.parseRewritePolicy(className, rewritePolicyElement, this);
      if (policy == null) {
         policy = this.buildRewritePolicy(className, rewritePolicyElement);
      }

      return policy;
   }

   private RewritePolicy buildRewritePolicy(String className, Element element) {
      try {
         RewritePolicy policy = (RewritePolicy)LoaderUtil.newInstanceOf(className);
         PropertySetter propSetter = new PropertySetter(policy);
         forEachElement(element.getChildNodes(), currentElement -> {
            if (currentElement.getTagName().equalsIgnoreCase("param")) {
               this.setParameter(currentElement, propSetter);
            }
         });
         propSetter.activate();
         return policy;
      } catch (XmlConfiguration.ConsumerException ex) {
         Throwable t = ex.getCause();
         if (t instanceof InterruptedException || t instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.error("Could not create an RewritePolicy. Reported error follows.", t);
      } catch (Exception oops) {
         if (oops instanceof InterruptedException || oops instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.error("Could not create an RewritePolicy. Reported error follows.", oops);
      }

      return null;
   }

   private void parseErrorHandler(Element element, Appender appender) {
      ErrorHandler eh = (ErrorHandler)OptionConverter.instantiateByClassName(this.subst(element.getAttribute("class")), ErrorHandler.class, null);
      if (eh != null) {
         eh.setAppender(appender);
         PropertySetter propSetter = new PropertySetter(eh);
         forEachElement(element.getChildNodes(), currentElement -> {
            String tagName = currentElement.getTagName();
            if (tagName.equals("param")) {
               this.setParameter(currentElement, propSetter);
            }
         });
         propSetter.activate();
         appender.setErrorHandler(eh);
      }
   }

   public void addFilter(final AtomicReference<Filter> ref, final Element filterElement) {
      Filter value = this.parseFilters(filterElement);
      ref.accumulateAndGet(value, FilterAdapter::addFilter);
   }

   public Filter parseFilters(final Element filterElement) {
      String className = this.subst(filterElement.getAttribute("class"));
      LOGGER.debug("Class name: [" + className + "]");
      Filter filter = this.manager.parseFilter(className, filterElement, this);
      if (filter == null) {
         filter = this.buildFilter(className, filterElement);
      }

      return filter;
   }

   private Filter buildFilter(final String className, final Element filterElement) {
      try {
         Filter filter = (Filter)LoaderUtil.newInstanceOf(className);
         PropertySetter propSetter = new PropertySetter(filter);
         forEachElement(filterElement.getChildNodes(), currentElement -> {
            switch (currentElement.getTagName()) {
               case "param":
                  this.setParameter(currentElement, propSetter);
            }
         });
         propSetter.activate();
         return filter;
      } catch (XmlConfiguration.ConsumerException ex) {
         Throwable t = ex.getCause();
         if (t instanceof InterruptedException || t instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.error("Could not create an Filter. Reported error follows.", t);
      } catch (Exception oops) {
         if (oops instanceof InterruptedException || oops instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.error("Could not create an Filter. Reported error follows.", oops);
      }

      return null;
   }

   private void parseCategory(final Element loggerElement) {
      String catName = this.subst(loggerElement.getAttribute("name"));
      boolean additivity = OptionConverter.toBoolean(this.subst(loggerElement.getAttribute("additivity")), true);
      LoggerConfig loggerConfig = this.getLogger(catName);
      if (loggerConfig == null) {
         loggerConfig = new LoggerConfig(catName, Level.ERROR, additivity);
         this.addLogger(catName, loggerConfig);
      } else {
         loggerConfig.setAdditive(additivity);
      }

      this.parseChildrenOfLoggerElement(loggerElement, loggerConfig, false);
   }

   private void parseRoot(final Element rootElement) {
      LoggerConfig root = this.getRootLogger();
      this.parseChildrenOfLoggerElement(rootElement, root, true);
   }

   private void parseChildrenOfLoggerElement(Element catElement, LoggerConfig loggerConfig, boolean isRoot) {
      PropertySetter propSetter = new PropertySetter(loggerConfig);
      loggerConfig.getAppenderRefs().clear();
      forEachElement(catElement.getChildNodes(), currentElement -> {
         switch (currentElement.getTagName()) {
            case "appender-ref":
               Appender appender = this.findAppenderByReference(currentElement);
               String refName = this.subst(currentElement.getAttribute("ref"));
               if (appender != null) {
                  LOGGER.debug("Adding appender named [{}] to loggerConfig [{}].", refName, loggerConfig.getName());
                  loggerConfig.addAppender(this.getAppender(refName), null, null);
               } else {
                  LOGGER.debug("Appender named [{}] not found.", refName);
               }
               break;
            case "level":
            case "priority":
               this.parseLevel(currentElement, loggerConfig, isRoot);
               break;
            case "param":
               this.setParameter(currentElement, propSetter);
               break;
            default:
               this.quietParseUnrecognizedElement(loggerConfig, currentElement, this.props);
         }
      });
      propSetter.activate();
   }

   public Layout parseLayout(final Element layoutElement) {
      String className = this.subst(layoutElement.getAttribute("class"));
      LOGGER.debug("Parsing layout of class: \"{}\"", className);
      Layout layout = this.manager.parseLayout(className, layoutElement, this);
      if (layout == null) {
         layout = this.buildLayout(className, layoutElement);
      }

      return layout;
   }

   private Layout buildLayout(final String className, final Element layout_element) {
      try {
         Layout layout = (Layout)LoaderUtil.newInstanceOf(className);
         PropertySetter propSetter = new PropertySetter(layout);
         forEachElement(layout_element.getChildNodes(), currentElement -> {
            String tagName = currentElement.getTagName();
            if (tagName.equals("param")) {
               this.setParameter(currentElement, propSetter);
            } else {
               try {
                  this.parseUnrecognizedElement(layout, currentElement, this.props);
               } catch (Exception ex) {
                  throw new XmlConfiguration.ConsumerException(ex);
               }
            }
         });
         propSetter.activate();
         return layout;
      } catch (Exception e) {
         Throwable cause = e.getCause();
         if (e instanceof InterruptedException
            || e instanceof InterruptedIOException
            || cause instanceof InterruptedException
            || cause instanceof InterruptedIOException) {
            Thread.currentThread().interrupt();
         }

         LOGGER.error("Could not create the Layout. Reported error follows.", e);
         return null;
      }
   }

   private void parseLevel(Element element, LoggerConfig logger, boolean isRoot) {
      String catName = logger.getName();
      if (isRoot) {
         catName = "root";
      }

      String priStr = this.subst(element.getAttribute("value"));
      LOGGER.debug("Level value for {} is [{}].", catName, priStr);
      if (!"inherited".equalsIgnoreCase(priStr) && !"null".equalsIgnoreCase(priStr)) {
         String className = this.subst(element.getAttribute("class"));
         org.apache.log4j.Level level;
         if ("".equals(className)) {
            level = OptionConverter.toLevel(priStr, DEFAULT_LEVEL);
         } else {
            level = OptionConverter.toLevel(className, priStr, DEFAULT_LEVEL);
         }

         logger.setLevel(level != null ? level.getVersion2Level() : null);
      } else if (isRoot) {
         LOGGER.error("Root level cannot be inherited. Ignoring directive.");
      } else {
         logger.setLevel(null);
      }

      LOGGER.debug("{} level set to {}", catName, logger.getLevel());
   }

   private void setParameter(Element element, PropertySetter propSetter) {
      String name = this.subst(element.getAttribute("name"));
      String value = element.getAttribute("value");
      value = this.subst(OptionConverter.convertSpecialChars(value));
      propSetter.setProperty(name, value);
   }

   private void parse(Element element) {
      String rootElementName = element.getTagName();
      if (!rootElementName.equals("log4j:configuration")) {
         if (!rootElementName.equals("configuration")) {
            LOGGER.error("DOM element is - not a <log4j:configuration> element.");
            return;
         }

         LOGGER.warn("The <configuration> element has been deprecated.");
         LOGGER.warn("Use the <log4j:configuration> element instead.");
      }

      String debugAttrib = this.subst(element.getAttribute("debug"));
      LOGGER.debug("debug attribute= \"" + debugAttrib + "\".");
      String status = "error";
      if (!debugAttrib.equals("") && !debugAttrib.equals("null")) {
         status = OptionConverter.toBoolean(debugAttrib, true) ? "debug" : "error";
      } else {
         LOGGER.debug("Ignoring debug attribute.");
      }

      String confDebug = this.subst(element.getAttribute("configDebug"));
      if (!confDebug.equals("") && !confDebug.equals("null")) {
         LOGGER.warn("The \"configDebug\" attribute is deprecated.");
         LOGGER.warn("Use the \"debug\" attribute instead.");
         status = OptionConverter.toBoolean(confDebug, true) ? "debug" : "error";
      }

      StatusConfiguration statusConfig = new StatusConfiguration().setStatus(status);
      statusConfig.initialize();
      String threshold = this.subst(element.getAttribute("threshold"));
      if (threshold != null) {
         Level level = OptionConverter.convertLevel(threshold.trim(), Level.ALL);
         this.addFilter(ThresholdFilter.createFilter(level, Result.NEUTRAL, Result.DENY));
      }

      forEachElement(element.getChildNodes(), currentElement -> {
         switch (currentElement.getTagName()) {
            case "category":
            case "logger":
               this.parseCategory(currentElement);
               break;
            case "root":
               this.parseRoot(currentElement);
               break;
            case "renderer":
               LOGGER.warn("Log4j 1 renderers are not supported by Log4j 2 and will be ignored.");
               break;
            case "throwableRenderer":
               LOGGER.warn("Log4j 1 throwable renderers are not supported by Log4j 2 and will be ignored.");
               break;
            case "categoryFactory":
            case "loggerFactory":
               LOGGER.warn("Log4j 1 logger factories are not supported by Log4j 2 and will be ignored.");
               break;
            case "appender":
               Appender appender = this.parseAppender(currentElement);
               this.appenderMap.put(appender.getName(), appender);
               this.addAppender(AppenderAdapter.adapt(appender));
               break;
            default:
               this.quietParseUnrecognizedElement(null, currentElement, this.props);
         }
      });
   }

   private String subst(final String value) {
      return this.getStrSubstitutor().replace(value);
   }

   public static void forEachElement(final NodeList list, final Consumer<Element> consumer) {
      IntStream.range(0, list.getLength()).mapToObj(list::item).filter(node -> node.getNodeType() == 1).forEach(node -> consumer.accept(node));
   }

   private static class ConsumerException extends RuntimeException {
      ConsumerException(final Exception ex) {
         super(ex);
      }
   }

   private interface ParseAction {
      Document parse(final DocumentBuilder parser) throws SAXException, IOException;
   }

   private static class SAXErrorHandler implements org.xml.sax.ErrorHandler {
      private static final Logger LOGGER = StatusLogger.getLogger();

      @Override
      public void error(final SAXParseException ex) {
         emitMessage("Continuable parsing error ", ex);
      }

      @Override
      public void fatalError(final SAXParseException ex) {
         emitMessage("Fatal parsing error ", ex);
      }

      @Override
      public void warning(final SAXParseException ex) {
         emitMessage("Parsing warning ", ex);
      }

      private static void emitMessage(final String msg, final SAXParseException ex) {
         LOGGER.warn("{} {} and column {}", msg, ex.getLineNumber(), ex.getColumnNumber());
         LOGGER.warn(ex.getMessage(), ex.getException());
      }
   }
}
