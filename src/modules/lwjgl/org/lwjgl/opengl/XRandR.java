package org.lwjgl.opengl;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.lwjgl.LWJGLUtil;

public class XRandR {
   private static XRandR.Screen[] current;
   private static String primaryScreenIdentifier;
   private static XRandR.Screen[] savedConfiguration;
   private static Map<String, XRandR.Screen[]> screens;
   private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");
   private static final Pattern SCREEN_HEADER_PATTERN = Pattern.compile("^(\\d+)x(\\d+)[+](\\d+)[+](\\d+)$");
   private static final Pattern SCREEN_MODELINE_PATTERN = Pattern.compile("^(\\d+)x(\\d+)$");
   private static final Pattern FREQ_PATTERN = Pattern.compile("^(\\d+[.]\\d+)(?:\\s*[*])?(?:\\s*[+])?$");
   private static final long XRANDR_TIMEOUT_MILLIS = 10000L;
   private static final long XRANDR_DESTROY_TIMEOUT_MILLIS = 1000L;

   private static void populate() {
      if (screens == null) {
         screens = new HashMap<>();
         primaryScreenIdentifier = null;

         try {
            List<String> outputLines = new ArrayList<>();
            int exitCode = executeCommand(new String[]{"xrandr", "-q"}, outputLines);
            if (exitCode != 0) {
               throw new IOException("xrandr -q exited with code " + exitCode + formatCommandOutput(outputLines));
            }

            List<XRandR.Screen> currentList = new ArrayList<>();
            List<XRandR.Screen> possibles = new ArrayList<>();
            String name = null;
            int[] currentScreenPosition = new int[2];

            for (String line : outputLines) {
               line = line.trim();
               if (line.isEmpty()) {
                  continue;
               }

               String[] sa = WHITESPACE_PATTERN.split(line);
               if (sa.length < 2) {
                  continue;
               }

               if ("connected".equals(sa[1])) {
                  if (name != null) {
                     screens.put(name, possibles.toArray(new XRandR.Screen[possibles.size()]));
                     possibles.clear();
                  }

                  name = sa[0];
                  if (sa.length > 2) {
                     if ("primary".equals(sa[2])) {
                        primaryScreenIdentifier = name;
                        if (sa.length > 3) {
                           parseScreenHeader(currentScreenPosition, sa[3]);
                        } else {
                           currentScreenPosition[0] = 0;
                           currentScreenPosition[1] = 0;
                        }
                     } else {
                        parseScreenHeader(currentScreenPosition, sa[2]);
                     }
                  } else {
                     currentScreenPosition[0] = 0;
                     currentScreenPosition[1] = 0;
                  }
               } else if ("disconnected".equals(sa[1])) {
                  if (name != null) {
                     screens.put(name, possibles.toArray(new XRandR.Screen[possibles.size()]));
                     name = null;
                  }
               } else if (name != null) {
                  Matcher m = SCREEN_MODELINE_PATTERN.matcher(sa[0]);
                  if (m.matches()) {
                     parseScreenModeline(possibles, currentList, name, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), sa, currentScreenPosition);
                  }
               }
            }

            if (name != null) {
               screens.put(name, possibles.toArray(new XRandR.Screen[possibles.size()]));
            }

            current = currentList.toArray(new XRandR.Screen[currentList.size()]);
            if (primaryScreenIdentifier == null) {
               long totalPixels = Long.MIN_VALUE;

               for (XRandR.Screen screen : current) {
                  if (1L * screen.width * screen.height > totalPixels) {
                     primaryScreenIdentifier = screen.name;
                     totalPixels = 1L * screen.width * screen.height;
                  }
               }
            }
         } catch (Throwable e) {
            LWJGLUtil.log("Exception in XRandR.populate(): " + e.getMessage());
            screens.clear();
            current = new XRandR.Screen[0];
         }
      }
   }

   public static XRandR.Screen[] getConfiguration() {
      populate();

      for (XRandR.Screen screen : current) {
         if (screen.name.equals(primaryScreenIdentifier)) {
            return new XRandR.Screen[]{screen};
         }
      }

      return (XRandR.Screen[])current.clone();
   }

   public static void setConfiguration(boolean disableOthers, XRandR.Screen... screens) {
      if (screens.length == 0) {
         throw new IllegalArgumentException("Must specify at least one screen");
      }

      if (savedConfiguration == null) {
         saveConfiguration();
      }

      List<String> cmd = new ArrayList<>();
      cmd.add("xrandr");
      if (disableOthers) {
         for (XRandR.Screen screen : current) {
            boolean disable = true;

            for (XRandR.Screen screen1 : screens) {
               if (screen1.name.equals(screen.name)) {
                  disable = false;
                  break;
               }
            }

            if (disable) {
               cmd.add("--output");
               cmd.add(screen.name);
               cmd.add("--off");
            }
         }
      }

      for (XRandR.Screen screen : screens) {
         screen.getArgs(cmd);
      }

      try {
         List<String> outputLines = new ArrayList<>();
         int exitCode = executeCommand(cmd.toArray(new String[cmd.size()]), outputLines);
         if (exitCode != 0) {
            LWJGLUtil.log("XRandR exception in setConfiguration(): xrandr exited with code " + exitCode + formatCommandOutput(outputLines));
            return;
         }

         for (String line : outputLines) {
            if (!line.trim().isEmpty()) {
               LWJGLUtil.log("Unexpected output from xrandr process: " + line);
            }
         }

         current = (XRandR.Screen[])screens.clone();
      } catch (IOException e) {
         LWJGLUtil.log("XRandR exception in setConfiguration(): " + e.getMessage());
      }
   }

   private static void saveConfiguration() {
      populate();
      savedConfiguration = (XRandR.Screen[])current.clone();
   }

   public static void restoreConfiguration() {
      if (savedConfiguration != null) {
         setConfiguration(true, savedConfiguration);
      }
   }

   public static String[] getScreenNames() {
      populate();
      return screens.keySet().toArray(new String[screens.size()]);
   }

   public static String getPrimaryScreenName() {
      populate();
      return primaryScreenIdentifier;
   }

   public static XRandR.Screen[] getResolutions(String name) {
      populate();
      return (XRandR.Screen[])screens.get(name).clone();
   }

   private static void parseScreenModeline(
      List<XRandR.Screen> allModes, List<XRandR.Screen> current, String name, int width, int height, String[] modeLine, int[] screenPosition
   ) {
      for (int i = 1; i < modeLine.length; i++) {
         String freqS = modeLine[i];
         if (!"+".equals(freqS)) {
            Matcher m = FREQ_PATTERN.matcher(freqS);
            if (!m.matches()) {
               LWJGLUtil.log("Frequency match failed: " + Arrays.toString(modeLine));
               return;
            }

            String freq = m.group(1);
            XRandR.Screen s = new XRandR.Screen(name, width, height, freq, 0, 0);
            if (freqS.contains("*")) {
               current.add(new XRandR.Screen(name, width, height, freq, screenPosition[0], screenPosition[1]));
               allModes.add(0, s);
            } else {
               allModes.add(s);
            }
         }
      }
   }

   private static void parseScreenHeader(int[] screenPosition, String resPos) {
      Matcher m = SCREEN_HEADER_PATTERN.matcher(resPos);
      if (!m.matches()) {
         screenPosition[0] = 0;
         screenPosition[1] = 0;
      } else {
         screenPosition[0] = Integer.parseInt(m.group(3));
         screenPosition[1] = Integer.parseInt(m.group(4));
      }
   }

   static XRandR.Screen DisplayModetoScreen(DisplayMode mode) {
      populate();
      XRandR.Screen primary = findPrimary(current);
      return new XRandR.Screen(primary.name, mode.getWidth(), mode.getHeight(), Integer.toString(mode.getFrequency()), primary.xPos, primary.yPos);
   }

   static DisplayMode ScreentoDisplayMode(XRandR.Screen... screens) {
      populate();
      XRandR.Screen primary = findPrimary(screens);
      return new DisplayMode(primary.width, primary.height, 24, primary.freq);
   }

   private static XRandR.Screen findPrimary(XRandR.Screen... screens) {
      for (XRandR.Screen screen : screens) {
         if (screen.name.equals(primaryScreenIdentifier)) {
            return screen;
         }
      }

      return screens[0];
   }

   private static int executeCommand(String[] command, List<String> outputLines) throws IOException {
      Path outputFile = Files.createTempFile("lwjgl-xrandr-", ".log");
      Process process = null;

      try {
         ProcessBuilder builder = new ProcessBuilder(command);
         builder.redirectErrorStream(true);
         builder.redirectOutput(outputFile.toFile());
         process = builder.start();
         closeQuietly(process.getOutputStream());
         waitForProcess(process);
         outputLines.addAll(readOutput(outputFile));
         return process.exitValue();
      } finally {
         if (process != null) {
            closeQuietly(process.getOutputStream());
            closeQuietly(process.getInputStream());
            closeQuietly(process.getErrorStream());
         }

         deleteQuietly(outputFile);
      }
   }

   private static void waitForProcess(Process process) throws IOException {
      try {
         if (!process.waitFor(XRANDR_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            terminateProcess(process);
            throw new IOException("Timed out waiting for xrandr process");
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         terminateProcess(process);
         throw new IOException("Interrupted while waiting for xrandr process", e);
      }
   }

   private static List<String> readOutput(Path outputFile) throws IOException {
      return Files.exists(outputFile) ? Files.readAllLines(outputFile) : Collections.<String>emptyList();
   }

   private static void deleteQuietly(Path outputFile) {
      if (outputFile != null) {
         try {
            Files.deleteIfExists(outputFile);
         } catch (IOException e) {
         }
      }
   }

   private static void terminateProcess(Process process) {
      if (process == null || !process.isAlive()) {
         return;
      }

      process.destroy();

      try {
         if (!process.waitFor(XRANDR_DESTROY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            process.waitFor(XRANDR_DESTROY_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
         }
      } catch (InterruptedException e) {
         Thread.currentThread().interrupt();
         if (process.isAlive()) {
            process.destroyForcibly();
         }
      }
   }

   private static void closeQuietly(Closeable closeable) {
      if (closeable != null) {
         try {
            closeable.close();
         } catch (IOException e) {
         }
      }
   }

   private static String formatCommandOutput(List<String> outputLines) {
      return outputLines.isEmpty() ? "" : ": " + outputLines;
   }

   public static class Screen implements Cloneable {
      public final String name;
      public final int width;
      public final int height;
      public final int freq;
      final String freqOriginal;
      public int xPos;
      public int yPos;

      Screen(String name, int width, int height, String freq, int xPos, int yPos) {
         this.name = name;
         this.width = width;
         this.height = height;
         this.freq = (int)Float.parseFloat(freq);
         this.freqOriginal = freq;
         this.xPos = xPos;
         this.yPos = yPos;
      }

      private void getArgs(List<String> argList) {
         argList.add("--output");
         argList.add(this.name);
         argList.add("--mode");
         argList.add(this.width + "x" + this.height);
         argList.add("--rate");
         argList.add(this.freqOriginal);
         argList.add("--pos");
         argList.add(this.xPos + "x" + this.yPos);
      }

      @Override
      public String toString() {
         return this.name + " " + this.width + "x" + this.height + " @ " + this.xPos + "x" + this.yPos + " with " + this.freqOriginal + "Hz";
      }
   }
}
