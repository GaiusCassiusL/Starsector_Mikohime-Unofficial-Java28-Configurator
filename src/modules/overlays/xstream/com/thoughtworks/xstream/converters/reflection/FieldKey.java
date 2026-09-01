package com.thoughtworks.xstream.converters.reflection;

public class FieldKey {
   private final String fieldName;
   private final Class declaringClass;
   private final int order;
   private int depth = -1;

   public FieldKey(String fieldName, Class declaringClass, int order) {
      if (fieldName != null && declaringClass != null) {
         this.fieldName = fieldName;
         this.declaringClass = declaringClass;
         this.order = order;
      } else {
         throw new IllegalArgumentException("fieldName or declaringClass is null");
      }
   }

   public String getFieldName() {
      return this.fieldName;
   }

   public Class getDeclaringClass() {
      return this.declaringClass;
   }

   public int getDepth() {
      if (this.depth == -1) {
         Class c = this.declaringClass;
         int i = 0;

         while (c.getSuperclass() != null) {
            i++;
            c = c.getSuperclass();
         }

         this.depth = i;
      }

      return this.depth;
   }

   public int getOrder() {
      return this.order;
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      }

      if (!(o instanceof FieldKey)) {
         return false;
      }

      FieldKey fieldKey = (FieldKey)o;
      return !this.declaringClass.equals(fieldKey.declaringClass) ? false : this.fieldName.equals(fieldKey.fieldName);
   }

   public int hashCode() {
      int result = this.fieldName.hashCode();
      return 29 * result + this.declaringClass.hashCode();
   }

   public String toString() {
      return "FieldKey{order="
         + this.order
         + ", writer="
         + this.getDepth()
         + ", declaringClass="
         + this.declaringClass
         + ", fieldName='"
         + this.fieldName
         + "'}";
   }
}
