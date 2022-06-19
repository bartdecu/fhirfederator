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
  ServerDesc setup;

  public Resources getResources() {
    return resources;
  }

  public void setResources(Resources resources) {
    this.resources = resources;
  }

  public static class Resources {
    List<ResourceConfig> default_;
    Map<String, List<ResourceConfig>> other;

    public List<ResourceConfig> getDefault() {
      return default_;
    }

    public void setDefault(List<ResourceConfig> default_) {
      this.default_ = default_;
    }

    public Map<String, List<ResourceConfig>> getOther() {
      return other;
    }

    public void setOther(Map<String, List<ResourceConfig>> other) {
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

  public static class ResourceConfig {

    String server;
    Integer maxOr;
    String read;
    private String create;
    private String update;

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

    public Integer getMaxOr() {
      return maxOr;
    }

    public void setServer(String url) {
      this.server = url;
    }

    public void setMaxOr(Integer maxOr) {
      this.maxOr = maxOr;
    }

    public String getCreate() {
      return create;
    }

    public String getUpdate() {
      return update;
    }
  }

  public ServerDesc getSetup() {
    return setup;
  }

  public void setSetup(ServerDesc setup) {
    this.setup = setup;
  }
}
