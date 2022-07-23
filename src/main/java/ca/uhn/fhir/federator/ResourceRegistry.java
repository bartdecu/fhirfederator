package ca.uhn.fhir.federator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;

public class ResourceRegistry {

  private final Map<String, ResourceConfig> map = new LinkedHashMap<>();
  private final ResourceConfig defaultConfig;

  public ResourceRegistry(ResourceConfig defaults) {
    this.defaultConfig = defaults;
  }

  ResourceConfig getServer4Resource(String resource) {
    ResourceConfig list = map.get(resource);
    if (list == null) {
      return defaultConfig;
    } else {
      return list;
    }
  }

  void putResourceConfig(String resource, ResourceConfig config) {
    map.put(resource, config);
  }

  public int getMaxOr4Resource(String resource) {
    ResourceConfig config = getServer4Resource(resource);

    Optional<Integer> min = Optional.ofNullable(config.getMaxOr());

    return min.orElse(10);
  }
}
