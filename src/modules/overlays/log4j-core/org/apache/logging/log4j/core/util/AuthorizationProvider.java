package org.apache.logging.log4j.core.util;

import java.net.URLConnection;

public interface AuthorizationProvider {
   void addAuthorization(URLConnection urlConnection);
}
