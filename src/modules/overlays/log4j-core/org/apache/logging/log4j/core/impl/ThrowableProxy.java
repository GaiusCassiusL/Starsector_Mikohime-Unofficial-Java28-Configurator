package org.apache.logging.log4j.core.impl;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.core.pattern.PlainTextRenderer;
import org.apache.logging.log4j.core.pattern.TextRenderer;
import org.apache.logging.log4j.util.StackLocatorUtil;

public class ThrowableProxy {
   private static final char EOL = '\n';
   private static final String EOL_STR = String.valueOf('\n');
   private final ThrowableProxy causeProxy;
   private int commonElementCount;
   private final ExtendedStackTraceElement[] extendedStackTrace;
   private final String localizedMessage;
   private final String message;
   private final String name;
   private final ThrowableProxy[] suppressedProxies;
   private final transient Throwable throwable;
   static final ThrowableProxy[] EMPTY_ARRAY = new ThrowableProxy[0];

   ThrowableProxy() {
      this.throwable = null;
      this.name = null;
      this.extendedStackTrace = ExtendedStackTraceElement.EMPTY_ARRAY;
      this.causeProxy = null;
      this.message = null;
      this.localizedMessage = null;
      this.suppressedProxies = EMPTY_ARRAY;
   }

   public ThrowableProxy(final Throwable throwable) {
      this(throwable, null);
   }

   ThrowableProxy(final Throwable throwable, final Set<Throwable> visited) {
      this.throwable = throwable;
      this.name = throwable.getClass().getName();
      this.message = throwable.getMessage();
      this.localizedMessage = throwable.getLocalizedMessage();
      Map<String, ThrowableProxyHelper.CacheEntry> map = new HashMap<>();
      Deque<Class<?>> stack = StackLocatorUtil.getCurrentStackTrace();
      this.extendedStackTrace = ThrowableProxyHelper.toExtendedStackTrace(this, stack, map, null, throwable.getStackTrace());
      Throwable throwableCause = throwable.getCause();
      Set<Throwable> causeVisited = new HashSet<>(1);
      this.causeProxy = throwableCause == null ? null : new ThrowableProxy(throwable, stack, map, throwableCause, visited, causeVisited);
      this.suppressedProxies = ThrowableProxyHelper.toSuppressedProxies(throwable, visited);
   }

   private ThrowableProxy(
      final Throwable parent,
      final Deque<Class<?>> stack,
      final Map<String, ThrowableProxyHelper.CacheEntry> map,
      final Throwable cause,
      final Set<Throwable> suppressedVisited,
      final Set<Throwable> causeVisited
   ) {
      causeVisited.add(cause);
      this.throwable = cause;
      this.name = cause.getClass().getName();
      this.message = this.throwable.getMessage();
      this.localizedMessage = this.throwable.getLocalizedMessage();
      this.extendedStackTrace = ThrowableProxyHelper.toExtendedStackTrace(this, stack, map, parent.getStackTrace(), cause.getStackTrace());
      Throwable causeCause = cause.getCause();
      this.causeProxy = causeCause != null && !causeVisited.contains(causeCause)
         ? new ThrowableProxy(parent, stack, map, causeCause, suppressedVisited, causeVisited)
         : null;
      this.suppressedProxies = ThrowableProxyHelper.toSuppressedProxies(cause, suppressedVisited);
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      }

      if (obj == null) {
         return false;
      }

      if (this.getClass() != obj.getClass()) {
         return false;
      }

      ThrowableProxy other = (ThrowableProxy)obj;
      if (this.causeProxy == null) {
         if (other.causeProxy != null) {
            return false;
         }
      } else if (!this.causeProxy.equals(other.causeProxy)) {
         return false;
      }

      if (this.commonElementCount != other.commonElementCount) {
         return false;
      }

      if (this.name == null) {
         if (other.name != null) {
            return false;
         }
      } else if (!this.name.equals(other.name)) {
         return false;
      }

      return !Arrays.equals(this.extendedStackTrace, other.extendedStackTrace) ? false : Arrays.equals(this.suppressedProxies, other.suppressedProxies);
   }

   public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause, final String suffix) {
      this.formatWrapper(sb, cause, null, PlainTextRenderer.getInstance(), suffix);
   }

   public void formatWrapper(final StringBuilder sb, final ThrowableProxy cause, final List<String> ignorePackages, final String suffix) {
      this.formatWrapper(sb, cause, ignorePackages, PlainTextRenderer.getInstance(), suffix);
   }

   public void formatWrapper(
      final StringBuilder sb, final ThrowableProxy cause, final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix
   ) {
      this.formatWrapper(sb, cause, ignorePackages, textRenderer, suffix, EOL_STR);
   }

   public void formatWrapper(
      final StringBuilder sb,
      final ThrowableProxy cause,
      final List<String> ignorePackages,
      final TextRenderer textRenderer,
      final String suffix,
      final String lineSeparator
   ) {
      ThrowableProxyRenderer.formatWrapper(sb, cause, ignorePackages, textRenderer, suffix, lineSeparator);
   }

   public ThrowableProxy getCauseProxy() {
      return this.causeProxy;
   }

   public String getCauseStackTraceAsString(final String suffix) {
      return this.getCauseStackTraceAsString(null, PlainTextRenderer.getInstance(), suffix, EOL_STR);
   }

   public String getCauseStackTraceAsString(final List<String> packages, final String suffix) {
      return this.getCauseStackTraceAsString(packages, PlainTextRenderer.getInstance(), suffix, EOL_STR);
   }

   public String getCauseStackTraceAsString(final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix) {
      return this.getCauseStackTraceAsString(ignorePackages, textRenderer, suffix, EOL_STR);
   }

   public String getCauseStackTraceAsString(final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix, final String lineSeparator) {
      StringBuilder sb = new StringBuilder();
      ThrowableProxyRenderer.formatCauseStackTrace(this, sb, ignorePackages, textRenderer, suffix, lineSeparator);
      return sb.toString();
   }

   public int getCommonElementCount() {
      return this.commonElementCount;
   }

   void setCommonElementCount(final int value) {
      this.commonElementCount = value;
   }

   public ExtendedStackTraceElement[] getExtendedStackTrace() {
      return this.extendedStackTrace;
   }

   public String getExtendedStackTraceAsString() {
      return this.getExtendedStackTraceAsString(null, PlainTextRenderer.getInstance(), "", EOL_STR);
   }

   public String getExtendedStackTraceAsString(final String suffix) {
      return this.getExtendedStackTraceAsString(null, PlainTextRenderer.getInstance(), suffix, EOL_STR);
   }

   public String getExtendedStackTraceAsString(final List<String> ignorePackages, final String suffix) {
      return this.getExtendedStackTraceAsString(ignorePackages, PlainTextRenderer.getInstance(), suffix, EOL_STR);
   }

   public String getExtendedStackTraceAsString(final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix) {
      return this.getExtendedStackTraceAsString(ignorePackages, textRenderer, suffix, EOL_STR);
   }

   public String getExtendedStackTraceAsString(
      final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix, final String lineSeparator
   ) {
      StringBuilder sb = new StringBuilder(1024);
      this.formatExtendedStackTraceTo(sb, ignorePackages, textRenderer, suffix, lineSeparator);
      return sb.toString();
   }

   public void formatExtendedStackTraceTo(
      final StringBuilder sb, final List<String> ignorePackages, final TextRenderer textRenderer, final String suffix, final String lineSeparator
   ) {
      ThrowableProxyRenderer.formatExtendedStackTraceTo(this, sb, ignorePackages, textRenderer, suffix, lineSeparator);
   }

   public String getLocalizedMessage() {
      return this.localizedMessage;
   }

   public String getMessage() {
      return this.message;
   }

   public String getName() {
      return this.name;
   }

   public StackTraceElement[] getStackTrace() {
      return this.throwable == null ? null : this.throwable.getStackTrace();
   }

   public ThrowableProxy[] getSuppressedProxies() {
      return this.suppressedProxies;
   }

   public String getSuppressedStackTrace(final String suffix) {
      ThrowableProxy[] suppressed = this.getSuppressedProxies();
      if (suppressed != null && suppressed.length != 0) {
         StringBuilder sb = new StringBuilder("Suppressed Stack Trace Elements:").append('\n');

         for (ThrowableProxy proxy : suppressed) {
            sb.append(proxy.getExtendedStackTraceAsString(suffix));
         }

         return sb.toString();
      } else {
         return "";
      }
   }

   public Throwable getThrowable() {
      return this.throwable;
   }

   @Override
   public int hashCode() {
      int prime = 31;
      int result = 1;
      result = 31 * result + (this.causeProxy == null ? 0 : this.causeProxy.hashCode());
      result = 31 * result + this.commonElementCount;
      result = 31 * result + (this.extendedStackTrace == null ? 0 : Arrays.hashCode(this.extendedStackTrace));
      result = 31 * result + (this.suppressedProxies == null ? 0 : Arrays.hashCode(this.suppressedProxies));
      return 31 * result + (this.name == null ? 0 : this.name.hashCode());
   }

   @Override
   public String toString() {
      String msg = this.message;
      return msg != null ? this.name + ": " + msg : this.name;
   }
}
