package ca.uhn.fhir.federator;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.hl7.fhir.r4.model.Identifier;

public class GetIdentifierHelper {

  private final Method getIdentifier;

  public GetIdentifierHelper(Class<?> clazz) {
    Method m = null;
    try {
      m = clazz.getMethod("getIdentifier");
    } catch (Exception e) {
      // doesn't matter
    }
    getIdentifier = m;
  }

  /* Call this method instead from your code. */
  @SuppressWarnings("unchecked")
  public List<Identifier> getIdentifier(Object resource) {
    if (getIdentifier != null) {
      try {

        return ((List<Identifier>) getIdentifier.invoke(resource));
      } catch (Exception e) {
        // doesn't matter
      }
    }
    return Collections.emptyList();
  }
}
