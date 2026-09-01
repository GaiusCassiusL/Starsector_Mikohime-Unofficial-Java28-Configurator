package com.thoughtworks.xstream.io;

import com.thoughtworks.xstream.converters.ErrorWriter;
import java.util.Iterator;

public abstract class ReaderWrapper implements ExtendedHierarchicalStreamReader {
   protected HierarchicalStreamReader wrapped;

   protected ReaderWrapper(HierarchicalStreamReader reader) {
      this.wrapped = reader;
   }

   public boolean hasMoreChildren() {
      return this.wrapped.hasMoreChildren();
   }

   public void moveDown() {
      this.wrapped.moveDown();
   }

   public void moveUp() {
      this.wrapped.moveUp();
   }

   public String getNodeName() {
      return this.wrapped.getNodeName();
   }

   public String getValue() {
      return this.wrapped.getValue();
   }

   public String getAttribute(String name) {
      return this.wrapped.getAttribute(name);
   }

   public String getAttribute(int index) {
      return this.wrapped.getAttribute(index);
   }

   public int getAttributeCount() {
      return this.wrapped.getAttributeCount();
   }

   public String getAttributeName(int index) {
      return this.wrapped.getAttributeName(index);
   }

   public Iterator getAttributeNames() {
      return this.wrapped.getAttributeNames();
   }

   public void appendErrors(ErrorWriter errorWriter) {
      this.wrapped.appendErrors(errorWriter);
   }

   public void close() {
      this.wrapped.close();
   }

   public String peekNextChild() {
      if (!(this.wrapped instanceof ExtendedHierarchicalStreamReader)) {
         throw new UnsupportedOperationException("peekNextChild");
      } else {
         return ((ExtendedHierarchicalStreamReader)this.wrapped).peekNextChild();
      }
   }

   public HierarchicalStreamReader underlyingReader() {
      return this.wrapped.underlyingReader();
   }
}
