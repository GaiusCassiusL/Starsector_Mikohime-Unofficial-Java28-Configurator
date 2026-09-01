package com.jcraft.jorbis;

class Residue2 extends Residue0 {
   @Override
   int inverse(Block vb, Object vl, float[][] in, int[] nonzero, int ch) {
      int i = 0;
      i = 0;

      while (i < ch && nonzero[i] == 0) {
         i++;
      }

      return i == ch ? 0 : _2inverse(vb, vl, in, ch);
   }
}
