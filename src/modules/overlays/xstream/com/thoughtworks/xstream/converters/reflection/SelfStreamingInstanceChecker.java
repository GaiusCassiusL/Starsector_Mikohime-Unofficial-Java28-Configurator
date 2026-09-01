package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.converters.Converter;

/** @deprecated */
public class SelfStreamingInstanceChecker extends com.thoughtworks.xstream.core.util.SelfStreamingInstanceChecker {
   public SelfStreamingInstanceChecker(Converter defaultConverter, Object xstream) {
      super(defaultConverter, xstream);
   }
}
