package ca.uhn.fhir.federator;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@EnableConfigurationProperties
@ConfigurationProperties(prefix = "federator")
@Configuration
public class FederatorProperties {

  public FederatorProperties() {
  }

  String my = null;

  Clients clients;
  SearchParams searchParams;

  public Clients getClients() {
    return clients;
  }

  public void setClients(Clients clients) {
    this.clients = clients;
  }

  public SearchParams getSearchParams() {
    return searchParams;
  }

  public void setSearchParams(SearchParams searchParams) {
    this.searchParams = searchParams;
  }

  public static class Clients {

    public List<String> getUrls() {
      return urls;
    }

    public void setUrls(List<String> urls) {
      this.urls = urls;
    }

    private List<String> urls = new ArrayList<>();
  }

  public static class SearchParams {

    public List<Map<String, String>> getMiscellaneous() {
      return miscellaneous;
    }

    public void setMiscellaneous(
        List<Map<String, String>> miscellaneous) {
      this.miscellaneous = miscellaneous;
    }

    private List<Map<String, String>> miscellaneous;
  }
}
