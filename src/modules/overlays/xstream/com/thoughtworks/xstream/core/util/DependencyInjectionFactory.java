package com.thoughtworks.xstream.core.util;

import com.thoughtworks.xstream.converters.reflection.ObjectAccessException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class DependencyInjectionFactory {
   public static Object newInstance(Class type, Object[] dependencies) {
      return newInstance(type, dependencies, null);
   }

   public static Object newInstance(Class type, Object[] dependencies, BitSet usedDependencies) {
      if (dependencies != null && dependencies.length > 63) {
         throw new IllegalArgumentException("More than 63 arguments are not supported");
      }

      Constructor bestMatchingCtor = null;
      ArrayList matchingDependencies = new ArrayList();
      List possibleMatchingDependencies = null;
      long usedDeps = 0L;
      long possibleUsedDeps = 0L;
      if (dependencies != null && dependencies.length > 0) {
         Constructor[] ctors = type.getConstructors();
         if (ctors.length > 1) {
            Arrays.sort(ctors, new DependencyInjectionFactory$1());
         }

         DependencyInjectionFactory.TypedValue[] typedDependencies = new DependencyInjectionFactory.TypedValue[dependencies.length];

         for (int i = 0; i < dependencies.length; i++) {
            Object dependency = dependencies[i];
            Class depType = dependency.getClass();
            if (depType.isPrimitive()) {
               depType = Primitives.box(depType);
            } else if (depType == TypedNull.class) {
               depType = ((TypedNull)dependency).getType();
               dependency = null;
            }

            typedDependencies[i] = new DependencyInjectionFactory.TypedValue(depType, dependency);
         }

         Constructor possibleCtor = null;
         int arity = Integer.MAX_VALUE;

         for (int i = 0; bestMatchingCtor == null && i < ctors.length; i++) {
            Constructor constructor = ctors[i];
            Class[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length <= dependencies.length) {
               if (parameterTypes.length == 0) {
                  if (possibleCtor == null) {
                     bestMatchingCtor = constructor;
                  }
                  break;
               }

               if (arity > parameterTypes.length) {
                  if (possibleCtor != null) {
                     continue;
                  }

                  arity = parameterTypes.length;
               }

               for (int j = 0; j < parameterTypes.length; j++) {
                  if (parameterTypes[j].isPrimitive()) {
                     parameterTypes[j] = Primitives.box(parameterTypes[j]);
                  }
               }

               matchingDependencies.clear();
               usedDeps = 0L;
               int j = 0;

               for (int k = 0; j < parameterTypes.length && parameterTypes.length + k - j <= typedDependencies.length; k++) {
                  if (parameterTypes[j].isAssignableFrom(typedDependencies[k].type)) {
                     matchingDependencies.add(typedDependencies[k].value);
                     usedDeps |= 1L << k;
                     if (++j == parameterTypes.length) {
                        bestMatchingCtor = constructor;
                        break;
                     }
                  }
               }

               if (bestMatchingCtor == null) {
                  boolean possible = true;
                  DependencyInjectionFactory.TypedValue[] deps = new DependencyInjectionFactory.TypedValue[typedDependencies.length];
                  System.arraycopy(typedDependencies, 0, deps, 0, deps.length);
                  matchingDependencies.clear();
                  usedDeps = 0L;

                  for (int jx = 0; jx < parameterTypes.length; jx++) {
                     int assignable = -1;

                     for (int k = 0; k < deps.length; k++) {
                        if (deps[k] != null) {
                           if (deps[k].type == parameterTypes[jx]) {
                              assignable = k;
                              break;
                           }

                           if (parameterTypes[jx].isAssignableFrom(deps[k].type)
                              && (assignable < 0 || deps[assignable].type != deps[k].type && deps[assignable].type.isAssignableFrom(deps[k].type))) {
                              assignable = k;
                           }
                        }
                     }

                     if (assignable < 0) {
                        possible = false;
                        break;
                     }

                     matchingDependencies.add(deps[assignable].value);
                     usedDeps |= 1L << assignable;
                     deps[assignable] = null;
                  }

                  if (possible && (possibleCtor == null || usedDeps < possibleUsedDeps)) {
                     possibleCtor = constructor;
                     possibleMatchingDependencies = (List)matchingDependencies.clone();
                     possibleUsedDeps = usedDeps;
                  }
               }
            }
         }

         if (bestMatchingCtor == null) {
            if (possibleCtor == null) {
               usedDeps = 0L;
               ObjectAccessException ex = new ObjectAccessException("Cannot construct type, none of the arguments match any constructor's parameters");
               ex.add("construction-type", type.getName());
               throw ex;
            }

            bestMatchingCtor = possibleCtor;
            matchingDependencies.clear();
            matchingDependencies.addAll(possibleMatchingDependencies);
            usedDeps = possibleUsedDeps;
         }
      }

      Throwable th = null;

      try {
         Object instance;
         if (bestMatchingCtor == null) {
            instance = type.newInstance();
         } else {
            instance = bestMatchingCtor.newInstance(matchingDependencies.toArray());
         }

         if (usedDependencies != null) {
            usedDependencies.clear();
            int i = 0;

            for (long l = 1L; l < usedDeps; i++) {
               if ((usedDeps & l) > 0L) {
                  usedDependencies.set(i);
               }

               l <<= 1;
            }
         }

         return instance;
      } catch (InstantiationException e) {
         th = e;
      } catch (IllegalAccessException e) {
         th = e;
      } catch (InvocationTargetException e) {
         th = e.getCause();
      } catch (SecurityException e) {
         th = e;
      } catch (ExceptionInInitializerError e) {
         th = e;
      }

      ObjectAccessException ex = new ObjectAccessException("Cannot construct type", th);
      ex.add("construction-type", type.getName());
      throw ex;
   }

   private static class TypedValue {
      final Class type;
      final Object value;

      public TypedValue(Class type, Object value) {
         this.type = type;
         this.value = value;
      }

      public String toString() {
         return this.type.getName() + ":" + this.value;
      }
   }
}
