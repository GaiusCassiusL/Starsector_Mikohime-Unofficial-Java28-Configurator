package com.thoughtworks.xstream.converters.reflection;

import com.thoughtworks.xstream.core.Caching;
import com.thoughtworks.xstream.core.JVM;
import com.thoughtworks.xstream.core.util.OrderRetainingMap;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FieldDictionary implements Caching {
   private static final FieldDictionary.DictionaryEntry OBJECT_DICTIONARY_ENTRY = new FieldDictionary.DictionaryEntry(
      Collections.EMPTY_MAP, Collections.EMPTY_MAP
   );
   private transient Map dictionaryEntries;
   private transient FieldDictionary.FieldUtil fieldUtil;
   private final FieldKeySorter sorter;

   public FieldDictionary() {
      this(new ImmutableFieldKeySorter());
   }

   public FieldDictionary(FieldKeySorter sorter) {
      this.sorter = sorter;
      this.init();
   }

   private void init() {
      this.dictionaryEntries = new HashMap();
      if (JVM.is15()) {
         try {
            this.fieldUtil = (FieldDictionary.FieldUtil)JVM.loadClassForName("com.thoughtworks.xstream.converters.reflection.FieldUtil15", true).newInstance();
         } catch (Exception var2) {
         }
      }

      if (this.fieldUtil == null) {
         this.fieldUtil = new FieldUtil14();
      }
   }

   /** @deprecated */
   public Iterator serializableFieldsFor(Class cls) {
      return this.fieldsFor(cls);
   }

   public Iterator fieldsFor(Class cls) {
      return this.buildMap(cls, true).values().iterator();
   }

   public Field field(Class cls, String name, Class definedIn) {
      Field field = this.fieldOrNull(cls, name, definedIn);
      if (field == null) {
         throw new MissingFieldException(cls.getName(), name);
      } else {
         return field;
      }
   }

   public Field fieldOrNull(Class cls, String name, Class definedIn) {
      Map fields = this.buildMap(cls, definedIn != null);
      return (Field)fields.get(definedIn != null ? new FieldKey(name, definedIn, -1) : name);
   }

   private Map buildMap(Class type, boolean tupleKeyed) {
      Class cls = type;
      FieldDictionary.DictionaryEntry lastDictionaryEntry;
      if (!Object.class.equals(cls) && cls != null) {
         lastDictionaryEntry = this.getDictionaryEntry(type);
      } else {
         lastDictionaryEntry = OBJECT_DICTIONARY_ENTRY;
      }

      if (lastDictionaryEntry == null) {
         List superClasses = new ArrayList();
         superClasses.add(cls);
         cls = cls.getSuperclass();

         while (lastDictionaryEntry == null) {
            if (!Object.class.equals(cls) && cls != null) {
               lastDictionaryEntry = this.getDictionaryEntry(cls);
            } else {
               lastDictionaryEntry = OBJECT_DICTIONARY_ENTRY;
            }

            if (lastDictionaryEntry == null) {
               superClasses.add(cls);
               cls = cls.getSuperclass();
            }
         }

         int i = superClasses.size();

         while (i-- > 0) {
            cls = (Class)superClasses.get(i);
            FieldDictionary.DictionaryEntry newDictionaryEntry = this.buildDictionaryEntryForClass(cls, lastDictionaryEntry);
            synchronized (this) {
               FieldDictionary.DictionaryEntry concurrentEntry = this.getDictionaryEntry(cls);
               if (concurrentEntry == null) {
                  this.dictionaryEntries.put(cls, newDictionaryEntry);
               } else {
                  newDictionaryEntry = concurrentEntry;
               }
            }

            lastDictionaryEntry = newDictionaryEntry;
         }
      }

      return tupleKeyed ? lastDictionaryEntry.getKeyedByFieldKey() : lastDictionaryEntry.getKeyedByFieldName();
   }

   private FieldDictionary.DictionaryEntry buildDictionaryEntryForClass(Class cls, FieldDictionary.DictionaryEntry lastDictionaryEntry) {
      Map keyedByFieldName = new HashMap(lastDictionaryEntry.getKeyedByFieldName());
      Map keyedByFieldKey = new OrderRetainingMap(lastDictionaryEntry.getKeyedByFieldKey());
      Field[] fields = cls.getDeclaredFields();
      if (JVM.reverseFieldDefinition()) {
         int i = fields.length >> 1;

         while (i-- > 0) {
            int idx = fields.length - i - 1;
            Field field = fields[i];
            fields[i] = fields[idx];
            fields[idx] = field;
         }
      }

      for (int i = 0; i < fields.length; i++) {
         Field field = fields[i];
         if (!this.fieldUtil.isSynthetic(field) || !field.getName().startsWith("$jacoco")) {
            if (!field.isAccessible()) {
               field.setAccessible(true);
            }

            FieldKey fieldKey = new FieldKey(field.getName(), field.getDeclaringClass(), i);
            Field existent = (Field)keyedByFieldName.get(field.getName());
            if (existent == null || (existent.getModifiers() & 8) != 0 || existent != null && (field.getModifiers() & 8) == 0) {
               keyedByFieldName.put(field.getName(), field);
            }

            keyedByFieldKey.put(fieldKey, field);
         }
      }

      Map sortedFieldKeys = this.sorter.sort(cls, keyedByFieldKey);
      return new FieldDictionary.DictionaryEntry(keyedByFieldName, sortedFieldKeys);
   }

   private synchronized FieldDictionary.DictionaryEntry getDictionaryEntry(Class cls) {
      return (FieldDictionary.DictionaryEntry)this.dictionaryEntries.get(cls);
   }

   public synchronized void flushCache() {
      this.dictionaryEntries.clear();
      if (this.sorter instanceof Caching) {
         ((Caching)this.sorter).flushCache();
      }
   }

   protected Object readResolve() {
      this.init();
      return this;
   }

   private static final class DictionaryEntry {
      private final Map keyedByFieldName;
      private final Map keyedByFieldKey;

      public DictionaryEntry(Map keyedByFieldName, Map keyedByFieldKey) {
         this.keyedByFieldName = keyedByFieldName;
         this.keyedByFieldKey = keyedByFieldKey;
      }

      public Map getKeyedByFieldName() {
         return this.keyedByFieldName;
      }

      public Map getKeyedByFieldKey() {
         return this.keyedByFieldKey;
      }
   }

   interface FieldUtil {
      boolean isSynthetic(Field var1);
   }
}
