package org.apache.logging.log4j.core.util;

import java.util.Map;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import org.apache.logging.log4j.plugins.di.Key;

public interface SecretKeyProvider {
   String CATEGORY = "KeyProvider";
   Key<Map<String, Supplier<SecretKeyProvider>>> PLUGIN_MAP_KEY = new Key<Map<String, Supplier<SecretKeyProvider>>>() {};

   SecretKey getSecretKey();
}
