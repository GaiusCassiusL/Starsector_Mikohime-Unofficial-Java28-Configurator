package com.thoughtworks.xstream.io;

public abstract class WriterWrapper implements ExtendedHierarchicalStreamWriter {
   protected HierarchicalStreamWriter wrapped;

   protected WriterWrapper(HierarchicalStreamWriter wrapped) {
      this.wrapped = wrapped;
   }

   public void startNode(String name) {
      this.wrapped.startNode(name);
   }

   public void startNode(String name, Class clazz) {
      ((ExtendedHierarchicalStreamWriter)this.wrapped).startNode(name, clazz);
   }

   public void endNode() {
      this.wrapped.endNode();
   }

   public void addAttribute(String key, String value) {
      this.wrapped.addAttribute(key, value);
   }

   public void setValue(String text) {
      this.wrapped.setValue(text);
   }

   public void flush() {
      this.wrapped.flush();
   }

   public void close() {
      this.wrapped.close();
   }

   public HierarchicalStreamWriter underlyingWriter() {
      return this.wrapped.underlyingWriter();
   }
}
