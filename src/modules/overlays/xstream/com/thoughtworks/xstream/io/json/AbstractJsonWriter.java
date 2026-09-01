package com.thoughtworks.xstream.io.json;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.core.util.FastStack;
import com.thoughtworks.xstream.io.AbstractWriter;
import com.thoughtworks.xstream.io.naming.NameCoder;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.mapper.Mapper;
import java.io.Externalizable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public abstract class AbstractJsonWriter extends AbstractWriter {
   public static final int DROP_ROOT_MODE = 1;
   public static final int STRICT_MODE = 2;
   public static final int EXPLICIT_MODE = 4;
   public static final int IEEE_754_MODE = 8;
   private static final int STATE_ROOT = 1;
   private static final int STATE_END_OBJECT = 2;
   private static final int STATE_START_OBJECT = 4;
   private static final int STATE_START_ATTRIBUTES = 8;
   private static final int STATE_NEXT_ATTRIBUTE = 16;
   private static final int STATE_END_ATTRIBUTES = 32;
   private static final int STATE_START_ELEMENTS = 64;
   private static final int STATE_NEXT_ELEMENT = 128;
   private static final int STATE_END_ELEMENTS = 256;
   private static final int STATE_SET_VALUE = 512;
   private static final Set NUMBER_TYPES = new HashSet<>(
      Arrays.asList(
         byte.class,
         Byte.class,
         short.class,
         Short.class,
         int.class,
         Integer.class,
         long.class,
         Long.class,
         float.class,
         Float.class,
         double.class,
         Double.class,
         BigInteger.class,
         BigDecimal.class
      )
   );
   private int mode;
   private FastStack stack = new FastStack(16);
   private int expectedStates;

   public AbstractJsonWriter() {
      this(new NoNameCoder());
   }

   public AbstractJsonWriter(int mode) {
      this(mode, new NoNameCoder());
   }

   public AbstractJsonWriter(NameCoder nameCoder) {
      this(0, nameCoder);
   }

   public AbstractJsonWriter(int mode, NameCoder nameCoder) {
      super(nameCoder);
      this.mode = (mode & 4) > 0 ? 4 : mode;
      this.stack.push(new AbstractJsonWriter.StackElement(null, 1));
      this.expectedStates = 4;
   }

   public void startNode(String name, Class clazz) {
      if (name == null) {
         throw new NullPointerException("name");
      }

      this.stack.push(new AbstractJsonWriter.StackElement(clazz, ((AbstractJsonWriter.StackElement)this.stack.peek()).status));
      this.handleCheckedStateTransition(4, name, null);
      this.expectedStates = 661;
   }

   public void startNode(String name) {
      this.startNode(name, null);
   }

   public void addAttribute(String name, String value) {
      this.handleCheckedStateTransition(16, name, value);
      this.expectedStates = 661;
   }

   public void setValue(String text) {
      Class type = ((AbstractJsonWriter.StackElement)this.stack.peek()).type;
      if ((type == Character.class || type == char.class) && "".equals(text)) {
         text = "\u0000";
      }

      this.handleCheckedStateTransition(512, null, text);
      this.expectedStates = 129;
   }

   public void endNode() {
      int size = this.stack.size();
      int nextState = size > 2 ? 128 : 1;
      this.handleCheckedStateTransition(nextState, null, null);
      this.stack.pop();
      ((AbstractJsonWriter.StackElement)this.stack.peek()).status = nextState;
      this.expectedStates = 4;
      if (size > 2) {
         this.expectedStates |= 129;
      }
   }

   private void handleCheckedStateTransition(int requiredState, String elementToAdd, String valueToAdd) {
      AbstractJsonWriter.StackElement stackElement = (AbstractJsonWriter.StackElement)this.stack.peek();
      if ((this.expectedStates & requiredState) == 0) {
         throw new AbstractJsonWriter.IllegalWriterStateException(stackElement.status, requiredState, elementToAdd);
      }

      int currentState = this.handleStateTransition(stackElement.status, requiredState, elementToAdd, valueToAdd);
      stackElement.status = currentState;
   }

   private int handleStateTransition(int currentState, int requiredState, String elementToAdd, String valueToAdd) {
      int size = this.stack.size();
      Class currentType = ((AbstractJsonWriter.StackElement)this.stack.peek()).type;
      boolean isArray = size > 1 && this.isArray(currentType);
      boolean isArrayElement = size > 1 && this.isArray(((AbstractJsonWriter.StackElement)this.stack.get(size - 2)).type);
      switch (currentState) {
         case 1:
            if (requiredState == 4) {
               currentState = this.handleStateTransition(64, 4, elementToAdd, null);
               return requiredState;
            }

            throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
         case 2:
            switch (requiredState) {
               case 1:
                  if (((this.mode & 1) == 0 || size > 2) && (this.mode & 4) == 0) {
                     this.endObject();
                  }

                  return requiredState;
               case 4:
                  currentState = this.handleStateTransition(currentState, 128, null, null);
                  currentState = this.handleStateTransition(currentState, 4, elementToAdd, null);
                  return requiredState;
               case 128:
                  this.nextElement();
                  return requiredState;
               default:
                  throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
            }
         case 4:
            switch (requiredState) {
               case 1:
               case 4:
               case 128:
               case 512:
                  if (!isArrayElement || (this.mode & 4) != 0) {
                     currentState = this.handleStateTransition(currentState, 8, null, null);
                     currentState = this.handleStateTransition(currentState, 32, null, null);
                  }

                  int var37 = 64;
                  switch (requiredState) {
                     case 1:
                     case 128:
                        var37 = this.handleStateTransition(var37, 512, null, null);
                        var37 = this.handleStateTransition(var37, requiredState, null, null);
                        break;
                     case 4:
                        var37 = this.handleStateTransition(var37, 4, elementToAdd, null);
                        break;
                     case 512:
                        var37 = this.handleStateTransition(var37, 512, null, valueToAdd);
                  }

                  return requiredState;
               case 8:
                  if ((this.mode & 4) != 0) {
                     this.startArray();
                  }

                  return requiredState;
               case 16:
                  if ((this.mode & 4) == 0 && isArray) {
                     return 4;
                  }

                  currentState = this.handleStateTransition(currentState, 8, null, null);
                  currentState = this.handleStateTransition(currentState, 16, elementToAdd, valueToAdd);
                  return requiredState;
               default:
                  throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
            }
         case 8:
            switch (requiredState) {
               case 16:
                  if (elementToAdd != null) {
                     String name = ((this.mode & 4) == 0 ? "@" : "") + elementToAdd;
                     this.startObject();
                     this.addLabel(this.encodeAttribute(name));
                     this.addValue(valueToAdd, AbstractJsonWriter.Type.STRING);
                  }

                  return requiredState;
            }
         case 16:
            switch (requiredState) {
               case 1:
                  currentState = this.handleStateTransition(currentState, 32, null, null);
                  currentState = this.handleStateTransition(currentState, 2, null, null);
                  currentState = this.handleStateTransition(currentState, 1, null, null);
                  return requiredState;
               case 4:
               case 512:
                  currentState = this.handleStateTransition(currentState, 32, null, null);
                  currentState = this.handleStateTransition(currentState, 64, null, null);
                  switch (requiredState) {
                     case 2:
                        currentState = this.handleStateTransition(currentState, 512, null, null);
                        currentState = this.handleStateTransition(currentState, 2, null, null);
                        break;
                     case 4:
                        currentState = this.handleStateTransition(currentState, 4, elementToAdd, (this.mode & 4) == 0 ? "" : null);
                        break;
                     case 512:
                        if ((this.mode & 4) == 0) {
                           this.addLabel(this.encodeNode("$"));
                        }

                        currentState = this.handleStateTransition(currentState, 512, null, valueToAdd);
                        if ((this.mode & 4) == 0) {
                           this.endObject();
                        }
                  }

                  return requiredState;
               case 16:
                  if (!isArray || (this.mode & 4) != 0) {
                     this.nextElement();
                     String name = ((this.mode & 4) == 0 ? "@" : "") + elementToAdd;
                     this.addLabel(this.encodeAttribute(name));
                     this.addValue(valueToAdd, AbstractJsonWriter.Type.STRING);
                  }

                  return requiredState;
               case 32:
                  if ((this.mode & 4) != 0) {
                     if (currentState == 16) {
                        this.endObject();
                     }

                     this.endArray();
                     this.nextElement();
                     this.startArray();
                  }

                  return requiredState;
               case 128:
                  currentState = this.handleStateTransition(currentState, 32, null, null);
                  currentState = this.handleStateTransition(currentState, 2, null, null);
                  return requiredState;
               default:
                  throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
            }
         case 32:
            switch (requiredState) {
               case 2:
                  currentState = this.handleStateTransition(64, 256, null, null);
                  currentState = this.handleStateTransition(currentState, 2, null, null);
                  break;
               case 64:
                  if ((this.mode & 4) == 0) {
                     this.nextElement();
                  }
                  break;
               default:
                  throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
            }

            return requiredState;
         case 128:
            switch (requiredState) {
               case 1:
                  currentState = this.handleStateTransition(currentState, 2, null, null);
                  currentState = this.handleStateTransition(currentState, 1, null, null);
                  return requiredState;
               case 2:
               case 128:
                  currentState = this.handleStateTransition(currentState, 256, null, null);
                  currentState = this.handleStateTransition(currentState, 2, null, null);
                  if ((this.mode & 4) == 0 && !isArray) {
                     this.endObject();
                  }

                  return requiredState;
               case 4:
                  this.nextElement();
                  if (!isArrayElement && (this.mode & 4) == 0) {
                     this.addLabel(this.encodeNode(elementToAdd));
                     if ((this.mode & 4) == 0 && isArray) {
                        this.startArray();
                     }

                     return requiredState;
                  }
                  break;
               case 256:
                  if ((this.mode & 4) == 0 && isArray) {
                     this.endArray();
                  }

                  return requiredState;
               default:
                  throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
            }
         case 64:
            switch (requiredState) {
               case 4:
                  if ((this.mode & 1) == 0 || size > 2) {
                     if (!isArrayElement || (this.mode & 4) != 0) {
                        if (!"".equals(valueToAdd)) {
                           this.startObject();
                        }

                        this.addLabel(this.encodeNode(elementToAdd));
                     }

                     if ((this.mode & 4) != 0) {
                        this.startArray();
                     }
                  }

                  if ((this.mode & 4) == 0 && isArray) {
                     this.startArray();
                  }

                  return requiredState;
               case 128:
               case 256:
                  if ((this.mode & 4) == 0) {
                     if (isArray) {
                        this.endArray();
                     } else {
                        this.endObject();
                     }
                  }

                  return requiredState;
               case 512:
                  if ((this.mode & 2) != 0 && size == 2) {
                     throw new ConversionException("Single value cannot be root element");
                  }

                  if (valueToAdd == null) {
                     if (currentType == Mapper.Null.class) {
                        this.addValue("null", AbstractJsonWriter.Type.NULL);
                     } else if ((this.mode & 4) == 0 && !isArray) {
                        this.startObject();
                        this.endObject();
                     }
                  } else if ((this.mode & 8) != 0 && (currentType == long.class || currentType == Long.class)) {
                     long longValue = Long.parseLong(valueToAdd);
                     if (longValue <= 9007199254740992L && longValue >= -9007199254740992L) {
                        this.addValue(valueToAdd, this.getType(currentType));
                     } else {
                        this.addValue(valueToAdd, AbstractJsonWriter.Type.STRING);
                     }
                  } else {
                     this.addValue(valueToAdd, this.getType(currentType));
                  }

                  return requiredState;
               default:
                  throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
            }
         case 256:
            switch (requiredState) {
               case 2:
                  if ((this.mode & 4) != 0) {
                     this.endArray();
                     this.endArray();
                     this.endObject();
                  }

                  return requiredState;
               default:
                  throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
            }
         case 512:
            switch (requiredState) {
               case 1:
                  currentState = this.handleStateTransition(currentState, 256, null, null);
                  currentState = this.handleStateTransition(currentState, 2, null, null);
                  currentState = this.handleStateTransition(currentState, 1, null, null);
                  return requiredState;
               case 128:
                  currentState = this.handleStateTransition(currentState, 256, null, null);
                  currentState = this.handleStateTransition(currentState, 2, null, null);
                  return requiredState;
               case 256:
                  if ((this.mode & 4) == 0 && isArray) {
                     this.endArray();
                  }

                  return requiredState;
               default:
                  throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
            }
         default:
            throw new AbstractJsonWriter.IllegalWriterStateException(currentState, requiredState, elementToAdd);
      }
   }

   protected AbstractJsonWriter.Type getType(Class clazz) {
      return clazz == Mapper.Null.class
         ? AbstractJsonWriter.Type.NULL
         : (
            clazz != Boolean.class && clazz != boolean.class
               ? (NUMBER_TYPES.contains(clazz) ? AbstractJsonWriter.Type.NUMBER : AbstractJsonWriter.Type.STRING)
               : AbstractJsonWriter.Type.BOOLEAN
         );
   }

   protected boolean isArray(Class clazz) {
      return clazz != null
         && (
            clazz.isArray()
               || Collection.class.isAssignableFrom(clazz)
               || Externalizable.class.isAssignableFrom(clazz)
               || Map.class.isAssignableFrom(clazz)
               || Entry.class.isAssignableFrom(clazz)
         );
   }

   protected abstract void startObject();

   protected abstract void addLabel(String var1);

   protected abstract void addValue(String var1, AbstractJsonWriter.Type var2);

   protected abstract void startArray();

   protected abstract void nextElement();

   protected abstract void endArray();

   protected abstract void endObject();

   private static class IllegalWriterStateException extends IllegalStateException {
      public IllegalWriterStateException(int from, int to, String element) {
         super("Cannot turn from state " + getState(from) + " into state " + getState(to) + (element == null ? "" : " for property " + element));
      }

      private static String getState(int state) {
         switch (state) {
            case 1:
               return "ROOT";
            case 2:
               return "END_OBJECT";
            case 4:
               return "START_OBJECT";
            case 8:
               return "START_ATTRIBUTES";
            case 16:
               return "NEXT_ATTRIBUTE";
            case 32:
               return "END_ATTRIBUTES";
            case 64:
               return "START_ELEMENTS";
            case 128:
               return "NEXT_ELEMENT";
            case 256:
               return "END_ELEMENTS";
            case 512:
               return "SET_VALUE";
            default:
               throw new IllegalArgumentException("Unknown state provided: " + state + ", cannot create message for IllegalWriterStateException");
         }
      }
   }

   private static class StackElement {
      final Class type;
      int status;

      public StackElement(Class type, int status) {
         this.type = type;
         this.status = status;
      }
   }

   public static class Type {
      public static AbstractJsonWriter.Type NULL = new AbstractJsonWriter.Type();
      public static AbstractJsonWriter.Type STRING = new AbstractJsonWriter.Type();
      public static AbstractJsonWriter.Type NUMBER = new AbstractJsonWriter.Type();
      public static AbstractJsonWriter.Type BOOLEAN = new AbstractJsonWriter.Type();
   }
}
