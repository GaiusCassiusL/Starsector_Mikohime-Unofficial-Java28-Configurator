package com.thoughtworks.xstream.core.util;

public class Pool {
   private final int initialPoolSize;
   private final int maxPoolSize;
   private final Pool.Factory factory;
   private transient Object[] pool;
   private transient int nextAvailable;
   private transient Object mutex = new Object();

   public Pool(int initialPoolSize, int maxPoolSize, Pool.Factory factory) {
      this.initialPoolSize = initialPoolSize;
      this.maxPoolSize = maxPoolSize;
      this.factory = factory;
   }

   public Object fetchFromPool() {
      synchronized (this.mutex) {
         if (this.pool == null) {
            this.pool = new Object[this.maxPoolSize];
            this.nextAvailable = this.initialPoolSize;

            while (this.nextAvailable > 0) {
               this.putInPool(this.factory.newInstance());
            }
         }

         while (this.nextAvailable == this.maxPoolSize) {
            try {
               this.mutex.wait();
            } catch (InterruptedException e) {
               throw new RuntimeException("Interrupted whilst waiting for a free item in the pool : " + e.getMessage());
            }
         }

         Object result = this.pool[this.nextAvailable++];
         if (result == null) {
            result = this.factory.newInstance();
            this.putInPool(result);
            this.nextAvailable++;
         }

         return result;
      }
   }

   protected void putInPool(Object object) {
      synchronized (this.mutex) {
         this.pool[--this.nextAvailable] = object;
         this.mutex.notify();
      }
   }

   private Object readResolve() {
      this.mutex = new Object();
      return this;
   }

   public interface Factory {
      Object newInstance();
   }
}
