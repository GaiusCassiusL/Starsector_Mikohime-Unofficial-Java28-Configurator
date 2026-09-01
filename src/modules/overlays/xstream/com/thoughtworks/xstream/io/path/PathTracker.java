package com.thoughtworks.xstream.io.path;

import java.util.HashMap;
import java.util.Map;

public class PathTracker {
   private int pointer;
   private int capacity;
   private String[] pathStack;
   private Map[] indexMapStack;
   private Path currentPath;

   public PathTracker() {
      this(16);
   }

   public PathTracker(int initialCapacity) {
      this.capacity = Math.max(1, initialCapacity);
      this.pathStack = new String[this.capacity];
      this.indexMapStack = new Map[this.capacity];
   }

   public void pushElement(String name) {
      if (this.pointer + 1 >= this.capacity) {
         this.resizeStacks(this.capacity * 2);
      }

      this.pathStack[this.pointer] = name;
      Map indexMap = this.indexMapStack[this.pointer];
      if (indexMap == null) {
         indexMap = new HashMap();
         this.indexMapStack[this.pointer] = indexMap;
      }

      if (indexMap.containsKey(name)) {
         indexMap.put(name, new Integer((Integer)indexMap.get(name) + 1));
      } else {
         indexMap.put(name, new Integer(1));
      }

      this.pointer++;
      this.currentPath = null;
   }

   public void popElement() {
      this.indexMapStack[this.pointer] = null;
      this.pathStack[this.pointer] = null;
      this.currentPath = null;
      this.pointer--;
   }

   public String peekElement() {
      return this.peekElement(0);
   }

   public String peekElement(int i) {
      if (i >= -this.pointer && i <= 0) {
         int idx = this.pointer + i - 1;
         Integer integer = (Integer)this.indexMapStack[idx].get(this.pathStack[idx]);
         int index = integer;
         String name;
         if (index > 1) {
            StringBuffer chunk = new StringBuffer(this.pathStack[idx].length() + 6);
            chunk.append(this.pathStack[idx]).append('[').append(index).append(']');
            name = chunk.toString();
         } else {
            name = this.pathStack[idx];
         }

         return name;
      } else {
         throw new ArrayIndexOutOfBoundsException(i);
      }
   }

   public int depth() {
      return this.pointer;
   }

   private void resizeStacks(int newCapacity) {
      String[] newPathStack = new String[newCapacity];
      Map[] newIndexMapStack = new Map[newCapacity];
      int min = Math.min(this.capacity, newCapacity);
      System.arraycopy(this.pathStack, 0, newPathStack, 0, min);
      System.arraycopy(this.indexMapStack, 0, newIndexMapStack, 0, min);
      this.pathStack = newPathStack;
      this.indexMapStack = newIndexMapStack;
      this.capacity = newCapacity;
   }

   public Path getPath() {
      if (this.currentPath == null) {
         String[] chunks = new String[this.pointer + 1];
         chunks[0] = "";
         int i = -this.pointer;

         while (++i <= 0) {
            String name = this.peekElement(i);
            chunks[i + this.pointer] = name;
         }

         this.currentPath = new Path(chunks);
      }

      return this.currentPath;
   }
}
