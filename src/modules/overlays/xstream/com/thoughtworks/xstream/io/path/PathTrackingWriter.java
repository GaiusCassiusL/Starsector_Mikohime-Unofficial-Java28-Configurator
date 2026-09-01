package com.thoughtworks.xstream.io.path;

import com.thoughtworks.xstream.io.AbstractWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.WriterWrapper;

public class PathTrackingWriter extends WriterWrapper {
   private final PathTracker pathTracker;
   private final boolean isNameEncoding;

   public PathTrackingWriter(HierarchicalStreamWriter writer, PathTracker pathTracker) {
      super(writer);
      this.isNameEncoding = writer.underlyingWriter() instanceof AbstractWriter;
      this.pathTracker = pathTracker;
   }

   public void startNode(String name) {
      this.pathTracker.pushElement(this.isNameEncoding ? ((AbstractWriter)this.wrapped.underlyingWriter()).encodeNode(name) : name);
      super.startNode(name);
   }

   public void startNode(String name, Class clazz) {
      this.pathTracker.pushElement(this.isNameEncoding ? ((AbstractWriter)this.wrapped.underlyingWriter()).encodeNode(name) : name);
      super.startNode(name, clazz);
   }

   public void endNode() {
      super.endNode();
      this.pathTracker.popElement();
   }
}
