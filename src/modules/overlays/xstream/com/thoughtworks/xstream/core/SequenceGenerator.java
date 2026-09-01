package com.thoughtworks.xstream.core;

public class SequenceGenerator implements ReferenceByIdMarshaller.IDGenerator {
   private int counter;

   public SequenceGenerator(int startsAt) {
      this.counter = startsAt;
   }

   public String next(Object item) {
      return String.valueOf(this.counter++);
   }
}
