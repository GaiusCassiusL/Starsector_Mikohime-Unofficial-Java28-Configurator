package org.apache.logging.log4j.core.lookup;

public interface InterpolatorFactory {
   Interpolator newInterpolator(final StrLookup defaultLookup);
}
