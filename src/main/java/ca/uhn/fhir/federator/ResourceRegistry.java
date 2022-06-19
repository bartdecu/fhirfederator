package ca.uhn.fhir.federator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.util.comparator.Comparators;

import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;

public class ResourceRegistry {

  private Map<String, List<ResourceConfig>> map = new LinkedHashMap<>();
  private List<ResourceConfig> defaultServers;

  public ResourceRegistry(List<ResourceConfig> defaults) {
    this.defaultServers = defaults;
  }

  List<ResourceConfig> getServer4Resource(String resource) {
    List<ResourceConfig> list = map.get(resource);
    if (list == null || list.isEmpty()) {
      return defaultServers;
    } else {
      return list;
    }
  }

  void putServer4Resource(String resource, ResourceConfig server) {
    List<ResourceConfig> list = map.get(resource);
    if (list == null) {
      map.put(resource, new ArrayList<>());
    }
    list = map.get(resource);
    if (list.contains(server)) {
      return;
    } else {
      list.add(server);
    }
  }

  public int getMaxOr4Resource(String resource) {
    List<ResourceConfig> configs = getServer4Resource(resource);

    Optional<Integer> min =
        configs.stream()
            .map(x -> x.getMaxOr() == null ? 10 : x.getMaxOr())
            .min(Comparators.comparable());

    return min.orElse(10);
  }
}
