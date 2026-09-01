package com.thoughtworks.xstream.mapper;

import com.thoughtworks.xstream.core.util.MemberDictionary;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ElementIgnoringMapper extends MapperWrapper {
   private final MemberDictionary fieldsToOmit = new MemberDictionary();
   private final Map unknownElementsToIgnore = new LinkedHashMap();

   public ElementIgnoringMapper(Mapper wrapped) {
      super(wrapped);
   }

   public void addElementsToIgnore(Pattern pattern) {
      this.unknownElementsToIgnore.put(pattern.pattern(), pattern);
   }

   public void omitField(Class definedIn, String fieldName) {
      this.fieldsToOmit.add(definedIn, fieldName);
   }

   public boolean shouldSerializeMember(Class definedIn, String fieldName) {
      if (this.fieldsToOmit.contains(definedIn, fieldName)) {
         return false;
      } else {
         return definedIn == Object.class && this.isIgnoredElement(fieldName) ? false : super.shouldSerializeMember(definedIn, fieldName);
      }
   }

   public boolean isIgnoredElement(String name) {
      if (!this.unknownElementsToIgnore.isEmpty()) {
         for (Pattern pattern : this.unknownElementsToIgnore.values()) {
            if (pattern.matcher(name).matches()) {
               return true;
            }
         }
      }

      return super.isIgnoredElement(name);
   }
}
