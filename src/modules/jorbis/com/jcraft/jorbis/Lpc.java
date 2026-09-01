package com.jcraft.jorbis;

class Lpc {
   Drft fft = new Drft();
   int ln;
   int m;

   static float lpc_from_data(float[] data, float[] lpc, int n, int m) {
      float[] aut = new float[m + 1];
      int j = m + 1;

      while (j-- != 0) {
         float d = 0.0F;

         for (int i = j; i < n; i++) {
            d += data[i] * data[i - j];
         }

         aut[j] = d;
      }

      float error = aut[0];

      for (int i = 0; i < m; i++) {
         float r = -aut[i + 1];
         if (error == 0.0F) {
            for (int k = 0; k < m; k++) {
               lpc[k] = 0.0F;
            }

            return 0.0F;
         }

         for (int var11 = 0; var11 < i; var11++) {
            r -= lpc[var11] * aut[i - var11];
         }

         r /= error;
         lpc[i] = r;

         for (j = 0; j < i / 2; j++) {
            float tmp = lpc[j];
            lpc[j] += r * lpc[i - 1 - j];
            lpc[i - 1 - j] = lpc[i - 1 - j] + r * tmp;
         }

         if (i % 2 != 0) {
            lpc[j] += lpc[j] * r;
         }

         error = (float)(error * (1.0 - r * r));
      }

      return error;
   }

   float lpc_from_curve(float[] curve, float[] lpc) {
      int n = this.ln;
      float[] work = new float[n + n];
      float fscale = (float)(0.5 / n);

      for (int i = 0; i < n; i++) {
         work[i * 2] = curve[i] * fscale;
         work[i * 2 + 1] = 0.0F;
      }

      work[n * 2 - 1] = curve[n - 1] * fscale;
      n *= 2;
      this.fft.backward(work);
      int var10 = 0;
      int j = n / 2;

      while (var10 < n / 2) {
         float temp = work[var10];
         work[var10++] = work[j];
         work[j++] = temp;
      }

      return lpc_from_data(work, lpc, n, this.m);
   }

   void init(int mapped, int m) {
      this.ln = mapped;
      this.m = m;
      this.fft.init(mapped * 2);
   }

   void clear() {
      this.fft.clear();
   }

   static float FAST_HYPOT(float a, float b) {
      return (float)Math.sqrt(a * a + b * b);
   }

   void lpc_to_curve(float[] curve, float[] lpc, float amp) {
      for (int i = 0; i < this.ln * 2; i++) {
         curve[i] = 0.0F;
      }

      if (amp != 0.0F) {
         for (int i = 0; i < this.m; i++) {
            curve[i * 2 + 1] = lpc[i] / (4.0F * amp);
            curve[i * 2 + 2] = -lpc[i] / (4.0F * amp);
         }

         this.fft.backward(curve);
         int l2 = this.ln * 2;
         float unit = (float)(1.0 / amp);
         curve[0] = (float)(1.0 / (curve[0] * 2.0F + unit));

         for (int i = 1; i < this.ln; i++) {
            float real = curve[i] + curve[l2 - i];
            float imag = curve[i] - curve[l2 - i];
            float a = real + unit;
            curve[i] = (float)(1.0 / FAST_HYPOT(a, imag));
         }
      }
   }
}
