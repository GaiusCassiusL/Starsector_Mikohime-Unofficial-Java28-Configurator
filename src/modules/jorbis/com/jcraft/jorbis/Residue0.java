package com.jcraft.jorbis;

import com.jcraft.jogg.Buffer;

class Residue0 extends FuncResidue {
   private static int[][][] _01inverse_partword = new int[2][][];
   static int[][] _2inverse_partword = null;

   @Override
   void pack(Object vr, Buffer opb) {
      Residue0.InfoResidue0 info = (Residue0.InfoResidue0)vr;
      int acc = 0;
      opb.write(info.begin, 24);
      opb.write(info.end, 24);
      opb.write(info.grouping - 1, 24);
      opb.write(info.partitions - 1, 6);
      opb.write(info.groupbook, 8);

      for (int j = 0; j < info.partitions; j++) {
         int i = info.secondstages[j];
         if (Util.ilog(i) > 3) {
            opb.write(i, 3);
            opb.write(1, 1);
            opb.write(i >>> 3, 5);
         } else {
            opb.write(i, 4);
         }

         acc += Util.icount(i);
      }

      for (int j = 0; j < acc; j++) {
         opb.write(info.booklist[j], 8);
      }
   }

   @Override
   Object unpack(Info vi, Buffer opb) {
      int acc = 0;
      Residue0.InfoResidue0 info = new Residue0.InfoResidue0();
      int begin = opb.read(24);
      int end = opb.read(24);
      int grouping = opb.read(24);
      int partitions = opb.read(6);
      int groupbook = opb.read(8);
      if (begin < 0 || end < 0 || grouping < 0 || partitions < 0 || groupbook < 0) {
         this.free_info(info);
         return null;
      }

      info.begin = begin;
      info.end = end;
      info.grouping = grouping + 1;
      info.partitions = partitions + 1;
      info.groupbook = groupbook;
      if (info.end < info.begin || info.grouping <= 0 || info.partitions <= 0 || info.partitions > info.secondstages.length || info.groupbook >= vi.books) {
         this.free_info(info);
         return null;
      }

      for (int j = 0; j < info.partitions; j++) {
         int cascade = opb.read(3);
         int hasHighBits = opb.read(1);
         if (cascade < 0 || hasHighBits < 0) {
            this.free_info(info);
            return null;
         }

         if (hasHighBits != 0) {
            int highBits = opb.read(5);
            if (highBits < 0) {
               this.free_info(info);
               return null;
            }

            cascade |= highBits << 3;
         }

         info.secondstages[j] = cascade;
         int stages = Util.icount(cascade);
         if (acc > info.booklist.length - stages) {
            this.free_info(info);
            return null;
         }

         acc += stages;
      }

      for (int j = 0; j < acc; j++) {
         info.booklist[j] = opb.read(8);
         if (info.booklist[j] < 0 || info.booklist[j] >= vi.books) {
            this.free_info(info);
            return null;
         }
      }

      return info;
   }

   @Override
   Object look(DspState vd, InfoMode vm, Object vr) {
      Residue0.InfoResidue0 info = (Residue0.InfoResidue0)vr;
      if (info.partitions <= 0
         || info.partitions > info.secondstages.length
         || info.grouping <= 0
         || info.begin < 0
         || info.end < info.begin
         || info.groupbook < 0
         || info.groupbook >= vd.fullbooks.length) {
         return null;
      }

      Residue0.LookResidue0 look = new Residue0.LookResidue0();
      int acc = 0;
      int maxstage = 0;
      look.info = info;
      look.map = vm.mapping;
      look.parts = info.partitions;
      look.fullbooks = vd.fullbooks;
      look.phrasebook = vd.fullbooks[info.groupbook];
      if (look.phrasebook == null || look.phrasebook.dim <= 0) {
         return null;
      }

      int dim = look.phrasebook.dim;
      look.partbooks = new int[look.parts][];

      for (int j = 0; j < look.parts; j++) {
         int i = info.secondstages[j];
         int stages = Util.ilog(i);
         if (stages != 0) {
            if (stages > maxstage) {
               maxstage = stages;
            }

            look.partbooks[j] = new int[stages];

            for (int k = 0; k < stages; k++) {
               if ((i & 1 << k) != 0) {
                  if (acc >= info.booklist.length) {
                     return null;
                  }

                  int book = info.booklist[acc++];
                  if (book < 0 || book >= look.fullbooks.length || look.fullbooks[book] == null) {
                     return null;
                  }

                  look.partbooks[j][k] = book;
               }
            }
         }
      }

      int maxPartvals = Integer.MAX_VALUE / dim;
      look.partvals = checkedPower(look.parts, dim, maxPartvals);
      if (look.partvals <= 0 || look.phrasebook.entries <= 0 || look.phrasebook.entries > look.partvals) {
         return null;
      }

      look.stages = maxstage;
      look.decodemap = new int[look.phrasebook.entries][];

      for (int j = 0; j < look.decodemap.length; j++) {
         int val = j;
         int mult = look.partvals / look.parts;
         look.decodemap[j] = new int[dim];

         for (int k = 0; k < dim; k++) {
            int deco = val / mult;
            val -= deco * mult;
            mult /= look.parts;
            look.decodemap[j][k] = deco;
         }
      }

      return look;
   }

   private static int checkedPower(int base, int exponent, int limit) {
      if (base <= 0 || exponent <= 0 || limit <= 0) {
         return -1;
      }

      long value = 1L;

      for (int i = 0; i < exponent; i++) {
         value *= base;
         if (value > limit) {
            return -1;
         }
      }

      return (int)value;
   }

   @Override
   void free_info(Object i) {
   }

   @Override
   void free_look(Object i) {
   }

   static synchronized int _01inverse(Block vb, Object vl, float[][] in, int ch, int decodepart) {
      Residue0.LookResidue0 look = (Residue0.LookResidue0)vl;
      Residue0.InfoResidue0 info = look.info;
      if (look.phrasebook == null || look.decodemap == null || look.parts <= 0 || info.grouping <= 0 || info.begin < 0 || info.end < info.begin) {
         return 0;
      }

      int samples_per_partition = info.grouping;
      int partitions_per_word = look.phrasebook.dim;
      if (partitions_per_word <= 0) {
         return 0;
      }

      int n = info.end - info.begin;
      int partvals = n / samples_per_partition;
      int partwords = (partvals + partitions_per_word - 1) / partitions_per_word;
      if (_01inverse_partword.length < ch) {
         _01inverse_partword = new int[ch][][];
      }

      for (int j = 0; j < ch; j++) {
         if (_01inverse_partword[j] == null || _01inverse_partword[j].length < partwords) {
            _01inverse_partword[j] = new int[partwords][];
         }
      }

      for (int s = 0; s < look.stages; s++) {
         int i = 0;

         for (int l = 0; i < partvals; l++) {
            if (s == 0) {
               for (int var20 = 0; var20 < ch; var20++) {
                  int temp = look.phrasebook.decode(vb.opb);
                  if (temp < 0 || temp >= look.decodemap.length) {
                     return 0;
                  }

                  _01inverse_partword[var20][l] = look.decodemap[temp];
                  if (_01inverse_partword[var20][l] == null) {
                     return 0;
                  }
               }
            }

            for (int k = 0; k < partitions_per_word && i < partvals; i++) {
               for (int var21 = 0; var21 < ch; var21++) {
                  int offset = info.begin + i * samples_per_partition;
                  if (offset < 0 || offset + samples_per_partition > in[var21].length) {
                     return 0;
                  }

                  int[] partword = _01inverse_partword[var21][l];
                  if (partword == null || k >= partword.length) {
                     return 0;
                  }

                  int index = partword[k];
                  if (index < 0 || index >= look.partbooks.length) {
                     return 0;
                  }

                  if ((info.secondstages[index] & 1 << s) != 0) {
                     if (look.partbooks[index] == null || s >= look.partbooks[index].length) {
                        return 0;
                     }

                     int stagebookIndex = look.partbooks[index][s];
                     if (stagebookIndex < 0 || stagebookIndex >= look.fullbooks.length) {
                        return 0;
                     }

                     CodeBook stagebook = look.fullbooks[stagebookIndex];
                     if (stagebook != null) {
                        if (decodepart == 0) {
                           if (stagebook.decodevs_add(in[var21], offset, vb.opb, samples_per_partition) == -1) {
                              return 0;
                           }
                        } else if (decodepart == 1 && stagebook.decodev_add(in[var21], offset, vb.opb, samples_per_partition) == -1) {
                           return 0;
                        }
                     }
                  }
               }

               k++;
            }
         }
      }

      return 0;
   }

   static synchronized int _2inverse(Block vb, Object vl, float[][] in, int ch) {
      Residue0.LookResidue0 look = (Residue0.LookResidue0)vl;
      Residue0.InfoResidue0 info = look.info;
      if (look.phrasebook == null || look.decodemap == null || look.parts <= 0 || info.grouping <= 0 || info.begin < 0 || info.end < info.begin) {
         return 0;
      }

      int samples_per_partition = info.grouping;
      int partitions_per_word = look.phrasebook.dim;
      if (partitions_per_word <= 0) {
         return 0;
      }

      int n = info.end - info.begin;
      int partvals = n / samples_per_partition;
      int partwords = (partvals + partitions_per_word - 1) / partitions_per_word;
      if (_2inverse_partword == null || _2inverse_partword.length < partwords) {
         _2inverse_partword = new int[partwords][];
      }

      for (int s = 0; s < look.stages; s++) {
         int i = 0;

         for (int l = 0; i < partvals; l++) {
            if (s == 0) {
               int temp = look.phrasebook.decode(vb.opb);
               if (temp < 0 || temp >= look.decodemap.length) {
                  return 0;
               }

               _2inverse_partword[l] = look.decodemap[temp];
               if (_2inverse_partword[l] == null) {
                  return 0;
               }
            }

            for (int k = 0; k < partitions_per_word && i < partvals; i++) {
               int offset = info.begin + i * samples_per_partition;
               if (offset < 0 || offset + samples_per_partition > in[0].length) {
                  return 0;
               }

               int[] partword = _2inverse_partword[l];
               if (partword == null || k >= partword.length) {
                  return 0;
               }

               int index = partword[k];
               if (index < 0 || index >= look.partbooks.length) {
                  return 0;
               }

               if ((info.secondstages[index] & 1 << s) != 0) {
                  if (look.partbooks[index] == null || s >= look.partbooks[index].length) {
                     return 0;
                  }

                  int stagebookIndex = look.partbooks[index][s];
                  if (stagebookIndex < 0 || stagebookIndex >= look.fullbooks.length) {
                     return 0;
                  }

                  CodeBook stagebook = look.fullbooks[stagebookIndex];
                  if (stagebook != null && stagebook.decodevv_add(in, offset, ch, vb.opb, samples_per_partition) == -1) {
                     return 0;
                  }
               }

               k++;
            }
         }
      }

      return 0;
   }

   @Override
   int inverse(Block vb, Object vl, float[][] in, int[] nonzero, int ch) {
      int used = 0;

      for (int i = 0; i < ch; i++) {
         if (nonzero[i] != 0) {
            in[used++] = in[i];
         }
      }

      return used != 0 ? _01inverse(vb, vl, in, used, 0) : 0;
   }

   class InfoResidue0 {
      int begin;
      int end;
      int grouping;
      int partitions;
      int groupbook;
      int[] secondstages = new int[64];
      int[] booklist = new int[256];
      float[] entmax = new float[64];
      float[] ampmax = new float[64];
      int[] subgrp = new int[64];
      int[] blimit = new int[64];
   }

   class LookResidue0 {
      Residue0.InfoResidue0 info;
      int map;
      int parts;
      int stages;
      CodeBook[] fullbooks;
      CodeBook phrasebook;
      int[][] partbooks;
      int partvals;
      int[][] decodemap;
      int postbits;
      int phrasebits;
      int frames;
   }
}
