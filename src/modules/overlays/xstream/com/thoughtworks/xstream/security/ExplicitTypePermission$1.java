package com.thoughtworks.xstream.security;

class ExplicitTypePermission$1 {
   ExplicitTypePermission$1(Class[] var1) {
      this.val$types = var1;
   }

   public String[] getNames() {
      if (this.val$types == null) {
         return null;
      }

      String[] names = new String[this.val$types.length];

      for (int i = 0; i < this.val$types.length; i++) {
         names[i] = this.val$types[i].getName();
      }

      return names;
   }
}
