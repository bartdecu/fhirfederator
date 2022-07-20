package ca.uhn.fhir.federator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;

import ca.uhn.fhir.rest.server.RestfulServer;

@SpringBootApplication(exclude = ElasticsearchRestClientAutoConfiguration.class)
public class Application {
  @Autowired FederatorProperties configuration;

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

  @Bean
  public ServletRegistrationBean<RestfulServer> fhirServlet() {
    return new ServletRegistrationBean<>(new FederatorRestfulServer(configuration), "/fhir/*");
  }
}
