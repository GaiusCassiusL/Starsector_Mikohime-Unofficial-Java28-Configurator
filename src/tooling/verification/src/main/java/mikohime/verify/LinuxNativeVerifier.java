package mikohime.verify;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LinuxNativeVerifier {
    private LinuxNativeVerifier() {
    }

    public static void main(String[] args) throws Exception {
        if (!System.getProperty("os.name", "").equals("Linux")) {
            throw new IllegalStateException("Linux native verification must run on Linux.");
        }
        Path mikohime = Path.of(args[0]).toAbsolutePath().normalize();
        Path natives = mikohime.resolve("linux");
        requireFile(natives.resolve("liblwjgl64.so"));
        requireFile(natives.resolve("libopenal64.so"));
        requireFile(natives.resolve("libjinput-linux64.so"));

        System.setProperty("org.lwjgl.librarypath", natives.toString());
        System.setProperty("net.java.games.input.librarypath", natives.toString());

        try (URLClassLoader lwjglLoader = loader(mikohime.resolve("lwjgl.jar"))) {
            Class<?> sys = Class.forName("org.lwjgl.Sys", true, lwjglLoader);
            String version = String.valueOf(sys.getMethod("getVersion").invoke(null));
            boolean is64Bit = (Boolean) sys.getMethod("is64Bit").invoke(null);
            Class.forName("org.lwjgl.opengl.LinuxDisplay", true, lwjglLoader);
            if (!"2.9.5".equals(version) || !is64Bit) {
                throw new IllegalStateException("Unexpected LWJGL native result: " + version + ":" + is64Bit);
            }
        }

        try (URLClassLoader jinputLoader = loader(mikohime.resolve("jinput.jar"))) {
            Class<?> plugin = Class.forName("net.java.games.input.LinuxEnvironmentPlugin", true, jinputLoader);
            boolean supported = (Boolean) plugin.getMethod("isSupported").invoke(plugin.getConstructor().newInstance());
            if (!supported) {
                throw new IllegalStateException("JInput Linux plugin did not report itself supported.");
            }
            Class<?> eventDevice = Class.forName("net.java.games.input.LinuxEventDevice", true, jinputLoader);
            var constructor = eventDevice.getDeclaredConstructor(String.class);
            constructor.setAccessible(true);
            try {
                constructor.newInstance("/dev/mikohime-verification-device-does-not-exist");
                throw new IllegalStateException("JInput unexpectedly opened the nonexistent verification device.");
            } catch (java.lang.reflect.InvocationTargetException expected) {
                if (!(expected.getCause() instanceof java.io.IOException)) {
                    throw expected;
                }
            }
        }

        System.out.println("Linux LWJGL 2.9.5 and JInput native initialization succeeded.");
    }

    private static URLClassLoader loader(Path jar) throws Exception {
        requireFile(jar);
        return new URLClassLoader(new URL[] {jar.toUri().toURL()}, ClassLoader.getPlatformClassLoader());
    }

    private static void requireFile(Path path) {
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Missing required file: " + path);
        }
    }
}
