package com.jcraft.jorbis;

import com.jcraft.jogg.Buffer;

class StaticCodeBook {
   int dim;
   int entries;
   int[] lengthlist;
   int maptype;
   int q_min;
   int q_delta;
   int q_quant;
   int q_sequencep;
   int[] quantlist;
   static final int VQ_FEXP = 10;
   static final int VQ_FMAN = 21;
   static final int VQ_FEXP_BIAS = 768;

   int pack(Buffer opb) {
      if (this.dim <= 0 || this.entries <= 0 || this.lengthlist == null || this.lengthlist.length < this.entries) {
         return -1;
      }

      boolean ordered = false;
      opb.write(5653314, 24);
      opb.write(this.dim, 16);
      opb.write(this.entries, 24);
      int i = 1;

      while (i < this.entries && this.lengthlist[i] >= this.lengthlist[i - 1]) {
         i++;
      }

      if (i == this.entries) {
         ordered = true;
      }

      if (ordered) {
         int count = 0;
         opb.write(1, 1);
         opb.write(this.lengthlist[0] - 1, 5);

         for (i = 1; i < this.entries; i++) {
            int _this = this.lengthlist[i];
            int _last = this.lengthlist[i - 1];
            if (_this > _last) {
               for (int j = _last; j < _this; j++) {
                  opb.write(i - count, Util.ilog(this.entries - count));
                  count = i;
               }
            }
         }

         opb.write(i - count, Util.ilog(this.entries - count));
      } else {
         opb.write(0, 1);
         i = 0;

         while (i < this.entries && this.lengthlist[i] != 0) {
            i++;
         }

         if (i == this.entries) {
            opb.write(0, 1);

            for (int var10 = 0; var10 < this.entries; var10++) {
               opb.write(this.lengthlist[var10] - 1, 5);
            }
         } else {
            opb.write(1, 1);

            for (int var11 = 0; var11 < this.entries; var11++) {
               if (this.lengthlist[var11] == 0) {
                  opb.write(0, 1);
               } else {
                  opb.write(1, 1);
                  opb.write(this.lengthlist[var11] - 1, 5);
               }
            }
         }
      }

      opb.write(this.maptype, 4);
      switch (this.maptype) {
         case 1:
         case 2:
            int quantvals = this.maptypeQuantvals();
            if (this.quantlist == null || this.q_quant <= 0 || this.q_quant > 16 || quantvals <= 0 || this.quantlist.length < quantvals) {
               return -1;
            } else {
               opb.write(this.q_min, 32);
               opb.write(this.q_delta, 32);
               opb.write(this.q_quant - 1, 4);
               opb.write(this.q_sequencep, 1);

               for (int var12 = 0; var12 < quantvals; var12++) {
                  opb.write(Math.abs(this.quantlist[var12]), this.q_quant);
               }
            }
         case 0:
            return 0;
         default:
            return -1;
      }
   }

   int unpack(Buffer opb) {
      if (opb.read(24) != 5653314) {
         this.clear();
         return -1;
      }

      this.dim = opb.read(16);
      this.entries = opb.read(24);
      if (this.dim <= 0 || this.entries <= 0) {
         this.clear();
         return -1;
      }

      int ordered = opb.read(1);
      if (ordered < 0) {
         this.clear();
         return -1;
      }

      switch (ordered) {
         case 0:
            this.lengthlist = new int[this.entries];
            int sparse = opb.read(1);
            if (sparse < 0) {
               this.clear();
               return -1;
            }

            if (sparse != 0) {
               for (int i = 0; i < this.entries; i++) {
                  int present = opb.read(1);
                  if (present < 0) {
                     this.clear();
                     return -1;
                  }

                  if (present != 0) {
                     int num = opb.read(5);
                     if (num == -1) {
                        this.clear();
                        return -1;
                     }

                     this.lengthlist[i] = num + 1;
                  } else {
                     this.lengthlist[i] = 0;
                  }
               }
            } else {
               for (int i = 0; i < this.entries; i++) {
                  int num = opb.read(5);
                  if (num == -1) {
                     this.clear();
                     return -1;
                  }

                  this.lengthlist[i] = num + 1;
               }
            }
            break;
         case 1:
            int length = opb.read(5);
            if (length < 0) {
              this.clear();
              return -1;
            }

            length++;
            this.lengthlist = new int[this.entries];

            for (int i = 0; i < this.entries; length++) {
              int remaining = this.entries - i;
              int num = opb.read(Util.ilog(remaining));
              if (num < 0 || num > remaining) {
                 this.clear();
                 return -1;
              }

               for (int j = 0; j < num; i++) {
                  this.lengthlist[i] = length;
                  j++;
               }
            }
            break;
         default:
            this.clear();
            return -1;
      }

      this.maptype = opb.read(4);
      if (this.maptype < 0) {
         this.clear();
         return -1;
      }

      switch (this.maptype) {
         case 1:
         case 2:
            long qMin = readInt32(opb);
            long qDelta = readInt32(opb);
            int qQuant = opb.read(4);
            this.q_sequencep = opb.read(1);
            if (qMin == Long.MIN_VALUE || qDelta == Long.MIN_VALUE || qQuant < 0 || this.q_sequencep < 0) {
               this.clear();
               return -1;
            }

            this.q_min = (int)qMin;
            this.q_delta = (int)qDelta;
            this.q_quant = qQuant + 1;
            if (this.q_quant <= 0) {
               this.clear();
               return -1;
            }

            int quantvals = this.maptypeQuantvals();
            if (quantvals <= 0) {
               this.clear();
               return -1;
            }

            this.quantlist = new int[quantvals];

            for (int var8 = 0; var8 < quantvals; var8++) {
               int quant = opb.read(this.q_quant);
               if (quant < 0) {
                  this.clear();
                  return -1;
               }

               this.quantlist[var8] = quant;
            }
         case 0:
            return 0;
         default:
            this.clear();
            return -1;
      }
   }

   private static int checkedProduct(int left, int right) {
      return left > 0 && right > 0 && left <= Integer.MAX_VALUE / right ? left * right : -1;
   }

   private static int comparePowToLimit(int base, int exponent, int limit) {
      long acc = 1L;

      for (int i = 0; i < exponent; i++) {
         acc *= base;
         if (acc > limit) {
            return 1;
         }
      }

      return acc < limit ? -1 : 0;
   }

   private static long readInt32(Buffer opb) {
      int low = opb.read(16);
      int high = opb.read(16);
      return low != -1 && high != -1 ? (long)(low | high << 16) : Long.MIN_VALUE;
   }

   private int maptypeQuantvals() {
      switch (this.maptype) {
         case 1:
            return this.maptype1_quantvals();
         case 2:
            return checkedProduct(this.entries, this.dim);
         default:
            return 0;
      }
   }

   private int maptype1_quantvals() {
      if (this.entries <= 0 || this.dim <= 0) {
         return -1;
      }

      int low = 1;
      int high = this.entries;
      int best = 0;

      while (low <= high) {
         int mid = low + (high - low) / 2;
         if (comparePowToLimit(mid, this.dim, this.entries) <= 0) {
            best = mid;
            low = mid + 1;
         } else {
            high = mid - 1;
         }
      }

      return best;
   }

   void clear() {
   }

   float[] unquantize() {
      if (this.maptype != 1 && this.maptype != 2) {
         return null;
      }

      int valueCount = checkedProduct(this.entries, this.dim);
      int quantvals = this.maptypeQuantvals();
      if (valueCount <= 0 || quantvals <= 0 || this.quantlist == null || this.quantlist.length < quantvals) {
         return null;
      }

      float mindel = float32_unpack(this.q_min);
      float delta = float32_unpack(this.q_delta);
      float[] r = new float[valueCount];
      switch (this.maptype) {
         case 1:
            for (int j = 0; j < this.entries; j++) {
               float last = 0.0F;
               int indexdiv = 1;

               for (int k = 0; k < this.dim; k++) {
                  int index = j / indexdiv % quantvals;
                  float val = this.quantlist[index];
                  val = Math.abs(val) * delta + mindel + last;
                  if (this.q_sequencep != 0) {
                     last = val;
                  }

                  r[j * this.dim + k] = val;
                  indexdiv *= quantvals;
               }
            }
            break;
         case 2:
            for (int j = 0; j < this.entries; j++) {
               float last = 0.0F;

               for (int k = 0; k < this.dim; k++) {
                  float val = this.quantlist[j * this.dim + k];
                  val = Math.abs(val) * delta + mindel + last;
                  if (this.q_sequencep != 0) {
                     last = val;
                  }

                  r[j * this.dim + k] = val;
               }
            }
      }

      return r;
   }

   static long float32_pack(float val) {
      int sign = 0;
      if (val < 0.0F) {
         sign = Integer.MIN_VALUE;
         val = -val;
      }

      int exp = (int)Math.floor(Math.log(val) / Math.log(2.0));
      int mant = (int)Math.rint(Math.pow(val, 20 - exp));
      exp = exp + 768 << 21;
      return sign | exp | mant;
   }

   static float float32_unpack(int val) {
      float mant = val & 2097151;
      float exp = (val & 2145386496) >>> 21;
      if ((val & -2147483648) != 0) {
         mant = -mant;
      }

      return ldexp(mant, (int)exp - 20 - 768);
   }

   static float ldexp(float foo, int e) {
      return (float)(foo * Math.pow(2.0, e));
   }
}
