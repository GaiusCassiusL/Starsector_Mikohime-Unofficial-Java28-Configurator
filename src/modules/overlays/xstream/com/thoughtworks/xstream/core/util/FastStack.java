package com.thoughtworks.xstream.core.util;

public final class FastStack {
   private Object[] stack;
   private int pointer;

   public FastStack(int initialCapacity) {
      this.stack = new Object[initialCapacity];
   }

   public Object push(Object value) {
      if (this.pointer + 1 >= this.stack.length) {
         this.resizeStack(this.stack.length * 2);
      }

      this.stack[this.pointer++] = value;
      return value;
   }

   public void popSilently() {
      this.stack[--this.pointer] = null;
   }

   public Object pop() {
      Object result = this.stack[--this.pointer];
      this.stack[this.pointer] = null;
      return result;
   }

   public Object peek() {
      return this.pointer == 0 ? null : this.stack[this.pointer - 1];
   }

   public Object replace(Object value) {
      Object result = this.stack[this.pointer - 1];
      this.stack[this.pointer - 1] = value;
      return result;
   }

   public void replaceSilently(Object value) {
      this.stack[this.pointer - 1] = value;
   }

   public int size() {
      return this.pointer;
   }

   public boolean hasStuff() {
      return this.pointer > 0;
   }

   public Object get(int i) {
      return this.stack[i];
   }

   private void resizeStack(int newCapacity) {
      Object[] newStack = new Object[newCapacity];
      System.arraycopy(this.stack, 0, newStack, 0, Math.min(this.pointer, newCapacity));
      this.stack = newStack;
   }

   public String toString() {
      StringBuffer result = new StringBuffer("[");

      for (int i = 0; i < this.pointer; i++) {
         if (i > 0) {
            result.append(", ");
         }

         result.append(this.stack[i]);
      }

      result.append(']');
      return result.toString();
   }
}
