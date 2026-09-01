package com.thoughtworks.xstream.converters.reflection;

class CGLIBEnhancedConverter$CGLIBFilteringReflectionProvider$1 implements ReflectionProvider.Visitor {
   CGLIBEnhancedConverter$CGLIBFilteringReflectionProvider$1(CGLIBEnhancedConverter.CGLIBFilteringReflectionProvider this$0, ReflectionProvider.Visitor var2) {
      this.this$0 = this$0;
      this.val$visitor = var2;
   }

   public void visit(String name, Class type, Class definedIn, Object value) {
      if (!name.startsWith("CGLIB$")) {
         this.val$visitor.visit(name, type, definedIn, value);
      }
   }
}
