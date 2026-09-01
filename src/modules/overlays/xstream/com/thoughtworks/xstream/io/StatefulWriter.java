package com.thoughtworks.xstream.io;

import com.thoughtworks.xstream.core.util.FastStack;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class StatefulWriter extends WriterWrapper {
   public static int STATE_OPEN = 0;
   public static int STATE_NODE_START = 1;
   public static int STATE_VALUE = 2;
   public static int STATE_NODE_END = 3;
   public static int STATE_CLOSED = 4;
   private transient int state = STATE_OPEN;
   private transient int balance;
   private transient FastStack attributes = new FastStack(16);

   public StatefulWriter(HierarchicalStreamWriter wrapped) {
      super(wrapped);
   }

   public void startNode(String name) {
      this.startNodeCommon();
      super.startNode(name);
   }

   public void startNode(String name, Class clazz) {
      this.startNodeCommon();
      super.startNode(name, clazz);
   }

   private void startNodeCommon() {
      this.checkClosed();
      if (this.state == STATE_VALUE) {
         throw new StreamException(new IllegalStateException("Opening node after writing text"));
      }

      this.state = STATE_NODE_START;
      this.balance++;
      this.attributes.push(new HashSet());
   }

   public void addAttribute(String name, String value) {
      this.checkClosed();
      if (this.state != STATE_NODE_START) {
         throw new StreamException(new IllegalStateException("Writing attribute '" + name + "' without an opened node"));
      }

      Set currentAttributes = (Set)this.attributes.peek();
      if (currentAttributes.contains(name)) {
         throw new StreamException(new IllegalStateException("Writing attribute '" + name + "' twice"));
      }

      currentAttributes.add(name);
      super.addAttribute(name, value);
   }

   public void setValue(String text) {
      this.checkClosed();
      if (this.state != STATE_NODE_START) {
         throw new StreamException(new IllegalStateException("Writing text without an opened node"));
      }

      this.state = STATE_VALUE;
      super.setValue(text);
   }

   public void endNode() {
      this.checkClosed();
      if (this.balance-- == 0) {
         throw new StreamException(new IllegalStateException("Unbalanced node"));
      }

      this.attributes.popSilently();
      this.state = STATE_NODE_END;
      super.endNode();
   }

   public void flush() {
      this.checkClosed();
      super.flush();
   }

   public void close() {
      if (this.state != STATE_NODE_END && this.state != STATE_OPEN) {
      }

      this.state = STATE_CLOSED;
      super.close();
   }

   private void checkClosed() {
      if (this.state == STATE_CLOSED) {
         throw new StreamException(new IOException("Writing on a closed stream"));
      }
   }

   public int state() {
      return this.state;
   }

   private Object readResolve() {
      this.attributes = new FastStack(16);
      return this;
   }
}
