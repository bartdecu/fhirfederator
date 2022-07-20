package ca.uhn.fhir.federator;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties
@ConfigurationProperties(prefix = "federator")
@Configuration
@ConfigurationPropertiesScan
public class FederatorProperties {

  List<ServerDesc> members;

  public List<ServerDesc> getMembers() {
    return members;
  }

  public void setMembers(List<ServerDesc> members) {
    this.members = members;
  }

  Resources resources;
  Setup setup;

  public Resources getResources() {
    return resources;
  }

  public void setResources(Resources resources) {
    this.resources = resources;
  }

  public static class ResourceConfig {
    List<List<String>> identifiers;
    List<ServerResourceConfig> locations;
    Integer maxOr;

    public void setMaxOr(Integer maxOr) {
      this.maxOr = maxOr;
    }

    public List<ServerResourceConfig> getLocations() {
      return locations;
    }

    public List<List<String>> getIdentifiers() {
      return identifiers;
    }

    public void setIdentifiers(List<List<String>> identifiers) {
      this.identifiers = identifiers;
    }

    public void setLocations(List<ServerResourceConfig> locations) {
      this.locations = locations;
    }

    public Integer getMaxOr() {
      return null;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((identifiers == null) ? 0 : identifiers.hashCode());
      result = prime * result + ((locations == null) ? 0 : locations.hashCode());
      result = prime * result + ((maxOr == null) ? 0 : maxOr.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      ResourceConfig other = (ResourceConfig) obj;
      if (identifiers == null) {
        if (other.identifiers != null) return false;
      } else if (!identifiers.equals(other.identifiers)) return false;
      if (locations == null) {
        if (other.locations != null) return false;
      } else if (!locations.equals(other.locations)) return false;
      if (maxOr == null) {
        return other.maxOr == null;
      } else return maxOr.equals(other.maxOr);
    }
  }

  public static class Resources {
    ResourceConfig default_;
    Map<String, ResourceConfig> other;

    public ResourceConfig getDefault() {
      return default_;
    }

    public void setDefault(ResourceConfig default_) {
      this.default_ = default_;
    }

    public Map<String, ResourceConfig> getOther() {
      return other;
    }

    public void setOther(Map<String, ResourceConfig> other) {
      this.other = other;
    }
  }

  public static class ServerDesc {
    String url;

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }
  }

  public static class ServerResourceConfig {

    private String server;
    private String read;
    private String create;
    private String update;
    private String delete;

    public void setDelete(String delete) {
      this.delete = delete;
    }

    public String getRead() {
      return read;
    }

    public void setRead(String read) {
      this.read = read;
    }

    public void setCreate(String create) {
      this.create = create;
    }

    public void setUpdate(String update) {
      this.update = update;
    }

    public String getServer() {
      return server;
    }

    public void setServer(String url) {
      this.server = url;
    }

    public String getCreate() {
      return create;
    }

    public String getUpdate() {
      return update;
    }

    public String getDelete() {
      return delete;
    }
  }

  public Setup getSetup() {
    return setup;
  }

  public void setSetup(Setup setup) {
    this.setup = setup;
  }

  public static class Setup {
    List<Package> packages;

    public List<Package> getPackages() {
      return packages;
    }

    public void setPackages(List<Package> packages) {
      this.packages = packages;
    }
  }

  public static class Package {
    String id;
    String version;
    String location;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getLocation() {
      return null;
    }
  }
}
