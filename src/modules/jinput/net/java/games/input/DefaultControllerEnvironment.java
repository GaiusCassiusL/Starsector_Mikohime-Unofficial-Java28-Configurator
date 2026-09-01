package net.java.games.input;

import java.io.File;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import net.java.games.util.plugins.Plugins;

class DefaultControllerEnvironment extends ControllerEnvironment {
   static String libPath;
   private ArrayList controllers;
   private Collection loadedPlugins = new ArrayList();

   static void loadLibrary(String lib_name) {
      AccessController.doPrivileged(new DefaultControllerEnvironment$1(lib_name));
   }

   static String getPrivilegedProperty(String property) {
      return (String) AccessController.doPrivileged(new DefaultControllerEnvironment$2(property));
   }

   static String getPrivilegedProperty(String property, String default_value) {
      return (String) AccessController.doPrivileged(new DefaultControllerEnvironment$3(property, default_value));
   }

   public DefaultControllerEnvironment() {
   }

   public Controller[] getControllers() {
      if (this.controllers == null) {
         this.controllers = new ArrayList();
         AccessController.doPrivileged(new DefaultControllerEnvironment$4(this));
         String pluginClasses = getPrivilegedProperty("jinput.plugins", "") + " " + getPrivilegedProperty("net.java.games.input.plugins", "");
         if (!getPrivilegedProperty("jinput.useDefaultPlugin", "true").toLowerCase().trim().equals("false")
            && !getPrivilegedProperty("net.java.games.input.useDefaultPlugin", "true").toLowerCase().trim().equals("false")) {
            String osName = getPrivilegedProperty("os.name", "").trim();
            if (osName.equals("Linux")) {
               pluginClasses = pluginClasses + " net.java.games.input.LinuxEnvironmentPlugin";
            } else if (osName.equals("Mac OS X")) {
               pluginClasses = pluginClasses + " net.java.games.input.OSXEnvironmentPlugin";
            } else if (osName.equals("Windows XP") || osName.equals("Windows Vista")) {
               pluginClasses = pluginClasses + " net.java.games.input.DirectAndRawInputEnvironmentPlugin";
            } else if (osName.equals("Windows 98") || osName.equals("Windows 2000")) {
               pluginClasses = pluginClasses + " net.java.games.input.DirectInputEnvironmentPlugin";
            } else if (osName.startsWith("Windows")) {
               System.out.println("WARNING: Found unknown Windows version: " + osName);
               System.out.println("Attempting to use default windows plug-in.");
               System.out.flush();
               pluginClasses = pluginClasses + " net.java.games.input.DirectAndRawInputEnvironmentPlugin";
            } else {
               System.out.println("Trying to use default plugin, OS name " + osName + " not recognised");
            }
         }

         StringTokenizer pluginClassTok = new StringTokenizer(pluginClasses, " \t\n\r\f,;:");

         while (pluginClassTok.hasMoreTokens()) {
            String className = pluginClassTok.nextToken();

            try {
               if (!this.loadedPlugins.contains(className)) {
                  System.out.println("Loading: " + className);
                  Class ceClass = Class.forName(className);
                  ControllerEnvironment ce = (ControllerEnvironment)ceClass.newInstance();
                  if (ce.isSupported()) {
                     this.addControllers(ce.getControllers());
                     this.loadedPlugins.add(ce.getClass().getName());
                  } else {
                     logln(ceClass.getName() + " is not supported");
                  }
               }
            } catch (Throwable e) {
               e.printStackTrace();
            }
         }
      }

      Controller[] ret = new Controller[this.controllers.size()];
      Iterator it = this.controllers.iterator();

      for (int i = 0; it.hasNext(); i++) {
         ret[i] = (Controller)it.next();
      }

      return ret;
   }

   private void scanControllers() {
      String pluginPathName = getPrivilegedProperty("jinput.controllerPluginPath");
      if (pluginPathName == null) {
         pluginPathName = "controller";
      }

      this.scanControllersAt(getPrivilegedProperty("java.home") + File.separator + "lib" + File.separator + pluginPathName);
      this.scanControllersAt(getPrivilegedProperty("user.dir") + File.separator + pluginPathName);
   }

   private void scanControllersAt(String path) {
      File file = new File(path);
      if (file.exists()) {
         try {
            Plugins plugins = new Plugins(file);
            Class[] envClasses = plugins.getExtends(ControllerEnvironment.class);

            for (int i = 0; i < envClasses.length; i++) {
               try {
                  ControllerEnvironment.logln("ControllerEnvironment " + envClasses[i].getName() + " loaded by " + envClasses[i].getClassLoader());
                  ControllerEnvironment ce = (ControllerEnvironment)envClasses[i].newInstance();
                  if (ce.isSupported()) {
                     this.addControllers(ce.getControllers());
                     this.loadedPlugins.add(ce.getClass().getName());
                  } else {
                     logln(envClasses[i].getName() + " is not supported");
                  }
               } catch (Throwable e) {
                  e.printStackTrace();
               }
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   private void addControllers(Controller[] c) {
      for (int i = 0; i < c.length; i++) {
         this.controllers.add(c[i]);
      }
   }

   public boolean isSupported() {
      return true;
   }

   // decompiler artifact: synthetic accessor for private method 'scanControllers', called from inner class
   static void access$000(DefaultControllerEnvironment obj) { obj.scanControllers(); }
}
