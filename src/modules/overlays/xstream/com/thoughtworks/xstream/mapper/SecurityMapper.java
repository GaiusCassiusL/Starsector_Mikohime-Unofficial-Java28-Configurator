package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.security.AnyTypePermission;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.TypePermission;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SecurityMapper extends MapperWrapper {
   private final List permissions;

   public SecurityMapper(Mapper wrapped) {
      this(wrapped, (TypePermission[])null);
   }

   public SecurityMapper(Mapper wrapped, TypePermission[] permissions) {
      super(wrapped);
      this.permissions = permissions == null ? new ArrayList() : new ArrayList<>(Arrays.asList(permissions));
   }

   public void addPermission(TypePermission permission) {
      if (permission.equals(NoTypePermission.NONE) || permission.equals(AnyTypePermission.ANY)) {
         this.permissions.clear();
      }

      this.permissions.add(0, permission);
   }

   public Class realClass(String elementName) {
      Class type = super.realClass(elementName);

      for (int i = 0; i < this.permissions.size(); i++) {
         TypePermission permission = (TypePermission)this.permissions.get(i);
         if (permission.allows(type)) {
            return type;
         }
      }

      throw new ForbiddenClassException(type);
   }
}
