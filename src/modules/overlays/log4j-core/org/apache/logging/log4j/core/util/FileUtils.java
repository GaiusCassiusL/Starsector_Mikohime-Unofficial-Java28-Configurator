package org.apache.logging.log4j.core.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.Objects;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.status.StatusLogger;

public final class FileUtils {
   private static final String PROTOCOL_FILE = "file";
   private static final String JBOSS_FILE = "vfsfile";
   private static final Logger LOGGER = StatusLogger.getLogger();

   private FileUtils() {
   }

   public static File fileFromUri(URI uri) {
      if (uri == null) {
         return null;
      }

      if (uri.isAbsolute()) {
         if ("vfsfile".equals(uri.getScheme())) {
            try {
               uri = new URI("file", uri.getSchemeSpecificPart(), uri.getFragment());
            } catch (URISyntaxException var4) {
            }
         }

         try {
            if ("file".equals(uri.getScheme())) {
               return new File(uri);
            }
         } catch (Exception ex) {
            LOGGER.warn("Invalid URI {}", uri);
         }
      } else {
         File file = new File(uri.toString());

         try {
            if (file.exists()) {
               return file;
            }

            String path = uri.getPath();
            return new File(path);
         } catch (Exception ex) {
            LOGGER.warn("Invalid URI {}", uri);
         }
      }

      return null;
   }

   public static boolean isFile(final URL url) {
      return url != null && (url.getProtocol().equals("file") || url.getProtocol().equals("vfsfile"));
   }

   public static String getFileExtension(final File file) {
      String fileName = file.getName();
      return fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0 ? fileName.substring(fileName.lastIndexOf(".") + 1) : null;
   }

   public static void mkdir(final File dir, final boolean createDirectoryIfNotExisting) throws IOException {
      if (!dir.exists()) {
         if (!createDirectoryIfNotExisting) {
            throw new IOException("The directory " + dir.getAbsolutePath() + " does not exist.");
         }

         if (!dir.mkdirs()) {
            throw new IOException("Could not create directory " + dir.getAbsolutePath());
         }
      }

      if (!dir.isDirectory()) {
         throw new IOException("File " + dir + " exists and is not a directory. Unable to create directory.");
      }
   }

   public static void makeParentDirs(final File file) throws IOException {
      File parent = Objects.requireNonNull(file, "file").getCanonicalFile().getParentFile();
      if (parent != null) {
         mkdir(parent, true);
      }
   }

   public static void defineFilePosixAttributeView(
      final Path path, final Set<PosixFilePermission> filePermissions, final String fileOwner, final String fileGroup
   ) throws IOException {
      PosixFileAttributeView view = Files.getFileAttributeView(path, PosixFileAttributeView.class);
      if (view != null) {
         UserPrincipalLookupService lookupService = FileSystems.getDefault().getUserPrincipalLookupService();
         if (fileOwner != null) {
            UserPrincipal userPrincipal = lookupService.lookupPrincipalByName(fileOwner);
            if (userPrincipal != null) {
               view.setOwner(userPrincipal);
            }
         }

         if (fileGroup != null) {
            GroupPrincipal groupPrincipal = lookupService.lookupPrincipalByGroupName(fileGroup);
            if (groupPrincipal != null) {
               view.setGroup(groupPrincipal);
            }
         }

         if (filePermissions != null) {
            view.setPermissions(filePermissions);
         }
      }
   }

   public static boolean isFilePosixAttributeViewSupported() {
      return FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
   }
}
