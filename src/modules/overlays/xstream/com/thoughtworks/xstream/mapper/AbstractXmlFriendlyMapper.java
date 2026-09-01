package com.thoughtworks.xstream.mapper;

/** @deprecated */
public class AbstractXmlFriendlyMapper extends MapperWrapper {
   private char dollarReplacementInClass = '-';
   private String dollarReplacementInField = "_DOLLAR_";
   private String underscoreReplacementInField = "__";
   private String noPackagePrefix = "default";

   protected AbstractXmlFriendlyMapper(Mapper wrapped) {
      super(wrapped);
   }

   protected String escapeClassName(String className) {
      className = className.replace('$', this.dollarReplacementInClass);
      if (className.charAt(0) == this.dollarReplacementInClass) {
         className = this.noPackagePrefix + className;
      }

      return className;
   }

   protected String unescapeClassName(String className) {
      if (className.startsWith(this.noPackagePrefix + this.dollarReplacementInClass)) {
         className = className.substring(this.noPackagePrefix.length());
      }

      return className.replace(this.dollarReplacementInClass, '$');
   }

   protected String escapeFieldName(String fieldName) {
      StringBuffer result = new StringBuffer();
      int length = fieldName.length();

      for (int i = 0; i < length; i++) {
         char c = fieldName.charAt(i);
         if (c == '$') {
            result.append(this.dollarReplacementInField);
         } else if (c == '_') {
            result.append(this.underscoreReplacementInField);
         } else {
            result.append(c);
         }
      }

      return result.toString();
   }

   protected String unescapeFieldName(String xmlName) {
      StringBuffer result = new StringBuffer();
      int length = xmlName.length();

      for (int i = 0; i < length; i++) {
         char c = xmlName.charAt(i);
         if (this.stringFoundAt(xmlName, i, this.underscoreReplacementInField)) {
            i += this.underscoreReplacementInField.length() - 1;
            result.append('_');
         } else if (this.stringFoundAt(xmlName, i, this.dollarReplacementInField)) {
            i += this.dollarReplacementInField.length() - 1;
            result.append('$');
         } else {
            result.append(c);
         }
      }

      return result.toString();
   }

   private boolean stringFoundAt(String name, int i, String replacement) {
      return name.length() >= i + replacement.length() && name.substring(i, i + replacement.length()).equals(replacement);
   }
}
