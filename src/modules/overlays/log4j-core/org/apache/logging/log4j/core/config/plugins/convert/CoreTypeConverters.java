package org.apache.logging.log4j.core.config.plugins.convert;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Provider;
import java.security.Security;
import java.time.ZoneId;
import java.util.Base64;
import java.util.UUID;
import java.util.Base64.Decoder;
import java.util.regex.Pattern;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.appender.rolling.action.Duration;
import org.apache.logging.log4j.core.util.CronExpression;
import org.apache.logging.log4j.plugins.Plugin;
import org.apache.logging.log4j.plugins.convert.TypeConverter;
import org.apache.logging.log4j.plugins.convert.TypeConverters;
import org.apache.logging.log4j.util.LoaderUtil;

public final class CoreTypeConverters {
   private static final Decoder decoder = Base64.getDecoder();

   @TypeConverters
   @Plugin
   public static class BigDecimalConverter implements TypeConverter<BigDecimal> {
      public BigDecimal convert(final String s) {
         return new BigDecimal(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class BigIntegerConverter implements TypeConverter<BigInteger> {
      public BigInteger convert(final String s) {
         return new BigInteger(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class ByteArrayConverter implements TypeConverter<byte[]> {
      private static final String PREFIX_0x = "0x";
      private static final String PREFIX_BASE64 = "Base64:";

      public byte[] convert(final String value) {
         byte[] bytes;
         if (value == null || value.isEmpty()) {
            bytes = new byte[0];
         } else if (value.startsWith("Base64:")) {
            String lexicalXSDBase64Binary = value.substring("Base64:".length());
            bytes = CoreTypeConverters.decoder.decode(lexicalXSDBase64Binary);
         } else if (value.startsWith("0x")) {
            String lexicalXSDHexBinary = value.substring("0x".length());
            bytes = HexConverter.parseHexBinary(lexicalXSDHexBinary);
         } else {
            bytes = value.getBytes(Charset.defaultCharset());
         }

         return bytes;
      }
   }

   @TypeConverters
   @Plugin
   public static class CharArrayConverter implements TypeConverter<char[]> {
      public char[] convert(final String s) {
         return s.toCharArray();
      }
   }

   @TypeConverters
   @Plugin
   public static class CharsetConverter implements TypeConverter<Charset> {
      public Charset convert(final String s) {
         return Charset.forName(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class ClassConverter implements TypeConverter<Class<?>> {
      public Class<?> convert(final String s) throws ClassNotFoundException {
         switch (s.toLowerCase()) {
            case "boolean":
               return boolean.class;
            case "byte":
               return byte.class;
            case "char":
               return char.class;
            case "double":
               return double.class;
            case "float":
               return float.class;
            case "int":
               return int.class;
            case "long":
               return long.class;
            case "short":
               return short.class;
            case "void":
               return void.class;
            default:
               return LoaderUtil.loadClass(s);
         }
      }
   }

   @TypeConverters
   @Plugin
   public static class CronExpressionConverter implements TypeConverter<CronExpression> {
      public CronExpression convert(final String s) throws Exception {
         return new CronExpression(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class DurationConverter implements TypeConverter<Duration> {
      public Duration convert(final String s) {
         return Duration.parse(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class FileConverter implements TypeConverter<File> {
      public File convert(final String s) {
         return new File(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class InetAddressConverter implements TypeConverter<InetAddress> {
      public InetAddress convert(final String s) throws Exception {
         return InetAddress.getByName(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class LevelConverter implements TypeConverter<Level> {
      public Level convert(final String s) {
         return Level.valueOf(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class PathConverter implements TypeConverter<Path> {
      public Path convert(final String s) throws Exception {
         return Paths.get(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class PatternConverter implements TypeConverter<Pattern> {
      public Pattern convert(final String s) {
         return Pattern.compile(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class SecurityProviderConverter implements TypeConverter<Provider> {
      public Provider convert(final String s) {
         return Security.getProvider(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class UriConverter implements TypeConverter<URI> {
      public URI convert(final String s) throws URISyntaxException {
         return new URI(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class UrlConverter implements TypeConverter<URL> {
      public URL convert(final String s) throws MalformedURLException {
         return new URL(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class UuidConverter implements TypeConverter<UUID> {
      public UUID convert(final String s) throws Exception {
         return UUID.fromString(s);
      }
   }

   @TypeConverters
   @Plugin
   public static class ZoneIdConverter implements TypeConverter<ZoneId> {
      public ZoneId convert(final String s) throws Exception {
         return ZoneId.of(s);
      }
   }
}
