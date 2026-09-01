package com.thoughtworks.xstream.io.binary;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

class ReaderDepthState {
   private static final String EMPTY_STRING = "";
   private ReaderDepthState.State current;

   public void push() {
      ReaderDepthState.State newState = new ReaderDepthState.State(null);
      newState.parent = this.current;
      this.current = newState;
   }

   public void pop() {
      this.current = this.current.parent;
   }

   public String getName() {
      return this.current.name;
   }

   public void setName(String name) {
      this.current.name = name;
   }

   public String getValue() {
      return this.current.value == null ? "" : this.current.value;
   }

   public void setValue(String value) {
      this.current.value = value;
   }

   public boolean hasMoreChildren() {
      return this.current.hasMoreChildren;
   }

   public void setHasMoreChildren(boolean hasMoreChildren) {
      this.current.hasMoreChildren = hasMoreChildren;
   }

   public void addAttribute(String name, String value) {
      ReaderDepthState.Attribute attribute = new ReaderDepthState.Attribute(null);
      attribute.name = name;
      attribute.value = value;
      if (this.current.attributes == null) {
         this.current.attributes = new ArrayList();
      }

      this.current.attributes.add(attribute);
   }

   public String getAttribute(String name) {
      if (this.current.attributes == null) {
         return null;
      }

      for (ReaderDepthState.Attribute attribute : this.current.attributes) {
         if (attribute.name.equals(name)) {
            return attribute.value;
         }
      }

      return null;
   }

   public String getAttribute(int index) {
      if (this.current.attributes == null) {
         return null;
      }

      ReaderDepthState.Attribute attribute = (ReaderDepthState.Attribute)this.current.attributes.get(index);
      return attribute.value;
   }

   public String getAttributeName(int index) {
      if (this.current.attributes == null) {
         return null;
      }

      ReaderDepthState.Attribute attribute = (ReaderDepthState.Attribute)this.current.attributes.get(index);
      return attribute.name;
   }

   public int getAttributeCount() {
      return this.current.attributes == null ? 0 : this.current.attributes.size();
   }

   public Iterator getAttributeNames() {
      if (this.current.attributes == null) {
         return Collections.EMPTY_SET.iterator();
      }

      Iterator attributeIterator = this.current.attributes.iterator();
      return new ReaderDepthState$1(this, attributeIterator);
   }

   private static class Attribute {
      String name;
      String value;

      private Attribute() {
      }
   }

   private static class State {
      String name;
      String value;
      List attributes;
      boolean hasMoreChildren;
      ReaderDepthState.State parent;

      private State() {
      }
   }
}
