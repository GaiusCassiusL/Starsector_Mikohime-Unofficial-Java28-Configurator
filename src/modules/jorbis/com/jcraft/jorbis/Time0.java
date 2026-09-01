package com.jcraft.jorbis;

import com.jcraft.jogg.Buffer;

class Time0 extends FuncTime {
   @Override
   void pack(Object i, Buffer opb) {
   }

   @Override
   Object unpack(Info vi, Buffer opb) {
      return "";
   }

   @Override
   Object look(DspState vd, InfoMode mi, Object i) {
      return "";
   }

   @Override
   void free_info(Object i) {
   }

   @Override
   void free_look(Object i) {
   }

   @Override
   int inverse(Block vb, Object i, float[] in, float[] out) {
      return 0;
   }
}
