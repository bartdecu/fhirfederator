package ca.uhn.fhir.federator;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.SearchParameter;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FederatorProperties.Package;
import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;
import ca.uhn.fhir.federator.FederatorProperties.Setup;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

// @Service
public class FederatorRestfulServer extends RestfulServer {

  public static final String ORG_HL_7_FHIR_R_4_MODEL_PREFIX = "org.hl7.fhir.r4.model.";
  private final FederatorProperties configuration;
  private static final Logger ourLog = LoggerFactory.getLogger(FederatorRestfulServer.class);

  public FederatorRestfulServer(FederatorProperties configuration) {
    this.configuration = configuration;

    // Create a context for the appropriate version
    setFhirContext(FhirContext.forR4());
    ClientRegistry cr = new ClientRegistry(
        configuration.getMembers().stream().map(x -> x.getUrl()).collect(Collectors.toList()),
        this.getFhirContext());
    ResourceRegistry rr = new ResourceRegistry(configuration.getResources().getDefault());
    for (Entry<String, ResourceConfig> entry : configuration.resources.other.entrySet()) {

      rr.putResourceConfig(entry.getKey(), entry.getValue());
    }
    SearchParam2FhirPathRegistry s2f = new SearchParam2FhirPathRegistry();
    List<SearchParameter> sps = new ArrayList<>();
    Setup setup = configuration.getSetup();
    if (setup != null) {
      for (Package package_ : setup.getPackages()) {
        sps.addAll(getSearchParametersFromNpmPackage(package_));
      }
    }

    sps.forEach(
        sp -> {
          sp.getBase()
              .forEach(
                  base -> {
                    s2f.put(base + "." + sp.getCode(), sp.getExpression());
                  });
        });

    File pagingFile = setupPagingFile();

    List<?> clazzes = getModelClasses();

    for (var object : clazzes) {
      try {
        ourLog.info("Loading {}", ((IBaseResource) object).getClass().getSimpleName());
        registerProvider(
            new FederatedReadProvider(
                this.getFhirContext(),
                cr,
                rr,
                (Class<? extends IBaseResource>) ((IBaseResource) object).getClass()));
        registerProvider(
            new FederatedCreateProvider(
                this.getFhirContext(),
                cr,
                rr,
                (Class<? extends IBaseResource>) ((IBaseResource) object).getClass()));
        registerProvider(
            new FederatedUpdateProvider(
                this.getFhirContext(),
                cr,
                rr,
                s2f,
                (Class<? extends IBaseResource>) ((IBaseResource) object).getClass()));
        registerProvider(
            new FederatedDeleteProvider(
                this.getFhirContext(),
                cr,
                rr,
                s2f,
                (Class<? extends IBaseResource>) ((IBaseResource) object).getClass()));

      } catch (IllegalArgumentException | SecurityException e) {
        ourLog.info(e.getMessage());
      }
    }

    registerProvider(new CapabilityStatementProvider(cr, rr));
    registerProvider(new FederatedSearchProvider(cr, rr, this.getFhirContext(), s2f));
    setPagingProvider(new MapDbPagingProvider(this.getFhirContext(), pagingFile, 10, 100));
    registerInterceptor(new FederatorInterceptor());
    registerInterceptor(new ResponseHighlighterInterceptor());
  }

  private File setupPagingFile() {
    File pagingFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "paging.db");

    if (pagingFile.exists()) {
      pagingFile.delete();
    }
    return pagingFile;
  }

  private List<?> getModelClasses() {
    var clazzes = getFhirContext().getResourceTypes().stream()
        .map(
            s -> {
              try {
                if ("List".equalsIgnoreCase(s)) {
                  return Class.forName(ORG_HL_7_FHIR_R_4_MODEL_PREFIX + s + "Resource")
                      .getDeclaredConstructor()
                      .newInstance();
                }
                return Class.forName(ORG_HL_7_FHIR_R_4_MODEL_PREFIX + s)
                    .getDeclaredConstructor()
                    .newInstance();
              } catch (ClassNotFoundException
                  | NoSuchMethodException
                  | InstantiationException
                  | IllegalAccessException
                  | InvocationTargetException e) {
                throw new RuntimeException(e.getMessage(), e);
              }
            })
        .collect(Collectors.toList());
    return clazzes;
  }

  private List<SearchParameter> getSearchParametersFromNpmPackage(Package package_) {
    List<SearchParameter> sps = new ArrayList<>();
    CloseableHttpResponse response = null;
    try {

      HttpClientBuilder b = HttpClientBuilder.create();
      CloseableHttpClient client = b.build();
      HttpUriRequest req = new HttpGet(
          Optional.<String>ofNullable(package_.getLocation())
              .orElse("https://packages2.fhir.org/packages")
              + "/"
              + Optional.<String>ofNullable(package_.getId()).orElse("hl7.fhir.r4.core")
              + "/"
              + Optional.<String>ofNullable(package_.getVersion()).orElse("4.0.1"));
      response = client.execute(req);
      if (!(response.getStatusLine().getStatusCode() < 200)
          && !(response.getStatusLine().getStatusCode() > 299)) {
        NpmPackage pkg = NpmPackage.fromPackage(response.getEntity().getContent());
        if (pkg.getFolders().containsKey("package")) {
          NpmPackage.NpmPackageFolder packageFolder = pkg.getFolders().get("package");

          for (String nextFile : packageFolder.listFiles()) {
            if (nextFile.toLowerCase(Locale.US).endsWith(".json")) {
              String input = new String(packageFolder.getContent().get(nextFile), StandardCharsets.UTF_8);
              IBaseResource resource = getFhirContext().newJsonParser().parseResource(input);
              if (resource instanceof SearchParameter) {
                sps.add((SearchParameter) resource);
              }
            }
          }
        }
      }

    } catch (Exception e) {
      ourLog.error(e.getMessage(), e);
    } finally {
      if (response != null) {
        try {
          response.close();
        } catch (IOException e) {
          // ignore
        }
      }
    }

    return sps;
  }
  /*
   * private List<SearchParameter> getSearchParametersFromConfig(
   * ResourceRegistry rr, ClientRegistry cr) {
   * List<SearchParameter> params = new ArrayList<>();
   * String url = configuration.getSetup().getUrl();
   * String dummy = rr.getServer4Resource("metadata").get(0).getServer();
   * do {
   * Bundle searchParameters =
   * cr.getClient(dummy)
   * .search()
   * .byUrl(url)
   * .accept(
   * "application/json;q=1.0;application/fhir+xml;q=1.0, application/fhir+json;q=1.0,"
   * + " application/xml+fhir;q=0.9, application/json+fhir;q=0.9")
   * .returnBundle(Bundle.class)
   * .execute();
   * 
   * searchParameters.getEntry().forEach(x -> params.add((SearchParameter)
   * x.getResource()));
   * 
   * url =
   * searchParameters.getLink(IBaseBundle.LINK_NEXT) == null
   * ? null
   * : searchParameters.getLink(IBaseBundle.LINK_NEXT).getUrl();
   * } while (url != null);
   * 
   * return params;
   * }
   */
}
