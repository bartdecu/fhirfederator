package ca.uhn.fhir.federator;

import ca.uhn.fhir.federator.FederatorProperties.ServerDesc;
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
  private static final Logger ourLog = LoggerFactory.getLogger(FederatorRestfulServer.class);

  public FederatorRestfulServer(FederatorProperties configuration) {

    // Create a context for the appropriate version
    setFhirContext(FhirContext.forR4());
    ClientRegistry cr =
        new ClientRegistry(
            configuration.getMembers().stream()
                .map(ServerDesc::getUrl)
                .collect(Collectors.toList()),
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
        sp -> sp.getBase().forEach(base -> s2f.put(base + "." + sp.getCode(), sp.getExpression())));

    File pagingFile = setupPagingFile();

    List<?> clazzes = getModelClasses();

    for (var object : clazzes) {
      try {
        ourLog.info("Loading {}", ((IBaseResource) object).getClass().getSimpleName());
        registerProvider(
            new FederatedReadProvider(
                this.getFhirContext(), cr, rr, ((IBaseResource) object).getClass()));
        registerProvider(
            new FederatedCreateProvider(
                this.getFhirContext(), cr, rr, ((IBaseResource) object).getClass()));
        registerProvider(
            new FederatedUpdateProvider(
                this.getFhirContext(), cr, rr, s2f, ((IBaseResource) object).getClass()));
        registerProvider(
            new FederatedDeleteProvider(
                this.getFhirContext(), cr, rr, s2f, ((IBaseResource) object).getClass()));

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
    return getFhirContext().getResourceTypes().stream()
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
  }

  private List<SearchParameter> getSearchParametersFromNpmPackage(Package package_) {
    List<SearchParameter> sps = new ArrayList<>();
    NpmPackage pkg = null;
    if (package_.getLocation()==null){
      String dir = System.getProperty("user.home")+File.separator +".fhir"+ File.separator + "packages";
      pkg = getNpmPackageFromFhirCache(dir, package_);
      if (pkg == null){
        String url = "https://packages2.fhir.org/packages";
        pkg = getNpmPackageFromFhirRegistry(url, package_);
      }

      
    }else{
      if(package_.getLocation().startsWith("http")){
        pkg = getNpmPackageFromFhirRegistry(package_.getLocation(),package_);
      } else {
        pkg = getNpmPackageFromFhirCache(package_.getLocation(), package_);    
      }
    }
    
    if (pkg!=null){
      getSearchParametersFromPackage(sps, pkg);
    }
    

    return sps;
  }
  

  private NpmPackage getNpmPackageFromFhirCache(String dir, Package package_) {
  
    String p = File.separator + Optional.ofNullable(package_.getId()).orElse("hl7.fhir.r4.core")+"#"+Optional.ofNullable(package_.getVersion()).orElse("4.0.1");
    try {
      NpmPackage pkg = NpmPackage.fromFolder(dir+p/*, null, new String[]{}*/);
      pkg.loadAllFiles();
      return pkg;
    } catch (IOException e) {
      
      ourLog.error(e.getMessage(), e);
      return null;
    }
    
  }

  private NpmPackage getNpmPackageFromFhirRegistry(String url, Package package_) {
    CloseableHttpResponse response = null;
    try {

      HttpClientBuilder b = HttpClientBuilder.create();
      CloseableHttpClient client = b.build();
      HttpUriRequest req =
          new HttpGet(
              url
                  + "/"
                  + Optional.ofNullable(package_.getId()).orElse("hl7.fhir.r4.core")
                  + "/"
                  + Optional.ofNullable(package_.getVersion()).orElse("4.0.1"));
      response = client.execute(req);
      if (!(response.getStatusLine().getStatusCode() < 200)
          && !(response.getStatusLine().getStatusCode() > 299)) {
         return  NpmPackage.fromPackage(response.getEntity().getContent());
        
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
    return null;
  }

  private void getSearchParametersFromPackage(List<SearchParameter> sps, NpmPackage pkg) {
    if (pkg.getFolders().containsKey("package")) {
      NpmPackage.NpmPackageFolder packageFolder = pkg.getFolders().get("package");

      for (String nextFile : packageFolder.listFiles()) {
        if (nextFile.toLowerCase(Locale.US).endsWith(".json")) {
          String input =
              new String(packageFolder.getContent().get(nextFile), StandardCharsets.UTF_8);
          IBaseResource resource = getFhirContext().newJsonParser().parseResource(input);
          if (resource instanceof SearchParameter) {
            sps.add((SearchParameter) resource);
          }
        }
      }
    }
  }
}
