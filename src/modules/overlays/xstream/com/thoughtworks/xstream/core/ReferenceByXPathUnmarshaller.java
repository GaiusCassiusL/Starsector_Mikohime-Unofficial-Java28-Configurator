package com.thoughtworks.xstream.core;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.io.AbstractReader;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingReader;
import com.thoughtworks.xstream.mapper.Mapper;

public class ReferenceByXPathUnmarshaller extends AbstractReferenceUnmarshaller {
   private PathTracker pathTracker = new PathTracker();
   protected boolean isNameEncoding;

   public ReferenceByXPathUnmarshaller(Object root, HierarchicalStreamReader reader, ConverterLookup converterLookup, Mapper mapper) {
      super(root, reader, converterLookup, mapper);
      this.reader = new PathTrackingReader(reader, this.pathTracker);
      this.isNameEncoding = reader.underlyingReader() instanceof AbstractReader;
   }

   protected Object getReferenceKey(String reference) {
      Path path = new Path(this.isNameEncoding ? ((AbstractReader)this.reader.underlyingReader()).decodeNode(reference) : reference);
      return reference.charAt(0) != '/' ? this.pathTracker.getPath().apply(path) : path;
   }

   protected Object getCurrentReferenceKey() {
      return this.pathTracker.getPath();
   }
}
