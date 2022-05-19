package ca.uhn.fhir.federator;

import java.io.IOException;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FhirUrlParser.SContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.BundleProviders;

public class FederatedSearchProvider {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FederatedSearchProvider.class);

  // Create a context
  // FhirContext ctx = FhirContext.forR4();
  private ClientRegistry cr;

  private ResourceRegistry rr;

  private FhirContext ctx;

  private SearchParam2FhirPathRegistry s2f;

  public FhirContext getCtx() {
    return ctx;
  }

  public void setCtx(FhirContext ctx) {
    this.ctx = ctx;
  }

  public FederatedSearchProvider(ClientRegistry cr, ResourceRegistry rr, FhirContext ctx,
      SearchParam2FhirPathRegistry s2f) {
    this.cr = cr;
    this.rr = rr;
    this.ctx = ctx;
    this.s2f = s2f;

  }

  /**
   * This method is a Patient search, but HAPI can not automatically
   * determine the resource type so it must be explicitly stated.
   */
  @Search(type = Patient.class)
  public Bundle searchForPatients(@RequiredParam(name = Patient.SP_IDENTIFIER) TokenParam identifier) {
    Bundle retVal = new Bundle();
    // perform search
    Set<String> keys = cr.getKeySet();
    Stream<String> stream = keys.parallelStream();

    Function<String, Stream<Patient>> f = new Function<String, Stream<Patient>>() {
      public Stream<Patient> apply(String prefix) {
        IGenericClient client = cr.getClient(prefix);
        Stream<Patient> out = null;
        if (identifier.getSystem().startsWith(prefix)) {

          Patient patient = client.read().resource(Patient.class).withId(identifier.getValue()).execute();
          out = Arrays.<Patient>asList(patient).stream();
        } else {

          Bundle bundle = client.search().forResource(Patient.class)
              .where(Patient.IDENTIFIER.exactly().systemAndIdentifier(identifier.getSystem(), identifier.getValue()))
              .returnBundle(Bundle.class).execute();

          out = bundle.getEntry().stream().filter(bec -> {
            return bec.getResource() instanceof Patient;
          }).map(bec -> ((Patient) bec.getResource()));

        }
        return out;
      }
    };

    List<Patient> result = stream.flatMap(f).collect(Collectors.<Patient>toList());

    result.stream().forEach(patient -> retVal.addEntry().setResource(patient));

    return retVal;
  }

  @Operation(name = "$doFederation", manualResponse = false, manualRequest = true, idempotent = true)
  public IBundleProvider manualInputAndOutput(HttpServletRequest theServletRequest,
      HttpServletResponse theServletResponse)
      throws IOException {
    String contentType = theServletRequest.getContentType();
    byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
    String requestFullPath = StringUtils.defaultString(theServletRequest.getRequestURI());
    String servletPath = StringUtils.defaultString(theServletRequest.getServletPath());
    StringBuffer requestUrl = theServletRequest.getRequestURL();
    String tenantAndResource = StringUtils.defaultString(theServletRequest.getPathInfo());
    if (ourLog.isTraceEnabled()) {
      ourLog.trace("Request FullPath: {}", requestFullPath);
      ourLog.trace("Servlet Path: {}", servletPath);
      ourLog.trace("Request Url: {}", requestUrl);
    }

    ourLog.info("Received call with content type {} and {} bytes", contentType, bytes.length);
    List<IBaseResource> result = doWithQueryAnalysis(theServletRequest, tenantAndResource);

    return BundleProviders.newList(result);

  }

  private List<IBaseResource> doWithQueryAnalysis(HttpServletRequest theServletRequest, String tenantAndResource)
      throws IOException {
    // TODO split tenant and resource
    String toParse;
    if (StringUtils.isNotBlank(theServletRequest.getQueryString())) {
      toParse = tenantAndResource + "?" + theServletRequest.getQueryString();
    } else {
      toParse = tenantAndResource;
    }

    CharStream inputCharStream = new ANTLRInputStream(new StringReader(toParse.substring(1)));
    TokenSource tokenSource = new FhirUrlLexer(inputCharStream);
    TokenStream inputTokenStream = new CommonTokenStream(tokenSource);
    FhirUrlParser parser = new FhirUrlParser(inputTokenStream);

    // parser.addErrorListener(new TestErrorListener());

    SContext context = parser.s();

    ourLog.info(TreeUtils.toPrettyTree(context, Arrays.asList(parser.getRuleNames())));

    ourLog.info(context.getText());

    FhirUrlAnalyser visitor = new FhirUrlAnalyser();

    context.accept(visitor);
    // ourLog.info(visitor.getResources().toString());
    // ourLog.info(visitor.getHttpParams().toString());

    // visitor.getResources().stream()
    // .forEach(r -> ourLog.info(TreeUtils.toPrettyTree(r.getParent(),
    // Arrays.asList(parser.getRuleNames()))));

    List<List<ParsedUrl>> perParameter = visitor.getHttpParams().stream()
        .map(httpParam -> visitor.getResourcesForHttpParam(httpParam).stream()
            .map(resourceInParam -> new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
            .collect(Collectors.toList()))
        .collect(Collectors.toList());

    List<List<IBaseResource>> toAnd = new ArrayList<>();

    for (int j = 0; j < perParameter.size(); j++) {

      List<ParsedUrl> urlsPerParameter = perParameter.get(j);
      Map<String, List<IBaseResource>> idMap = new HashMap<>();

      for (int i = (urlsPerParameter.size() - 1); i >= 0; i--) {
        ParsedUrl url = urlsPerParameter.get(i);
        String resource = url.getResource();
        List<ParsedUrl> executableUrls = createExecutableUrl(idMap, url, rr );
        for (ParsedUrl executableUrl : executableUrls) {
          List<IBaseResource> result = null;
          if (StringUtils.isEmpty(executableUrl.getValue())) {
            result = Collections.emptyList();
          } else {
            List<ResourceConfig> servers = rr.getServer4Resource(resource);
            Stream<ResourceConfig> stream = servers.parallelStream();
            result = stream.flatMap(resourceConfig -> {

              IGenericClient client = cr.getClient(resourceConfig.getServer());
              String completeUrl;
              completeUrl = resourceConfig.getServer() + "/" + executableUrl.toString();
              ourLog.info("Client request Url: {}", completeUrl);
              return getResultsForURL(client, completeUrl).stream();
            }).collect(Collectors.<IBaseResource>toList());
          }
          List<IBaseResource> list = idMap.get(resource);
          if (list == null || list.isEmpty()) {
            idMap.put(resource, result);
          } else {
            list.addAll(result);
          }
        }

      }

      toAnd.add(idMap.get(urlsPerParameter.get(0).getResource()));
    }

    if (toAnd.size() == 1) {
      return toAnd.get(0);
    } else {
      List<List<IBaseResource>> result = new ArrayList<>();
      result.add(toAnd.get(0));
      for (int i = 1; i < toAnd.size(); i++) {
        result.add(0, toAnd.get(i).stream()
            .distinct()
            .filter(x -> result.get(0).stream().map(y -> new IBaseResourceIdentifierComparator().compare(x, y) == 0)
                .reduce((a, b) -> a || b).orElse(false))
            .collect(Collectors.toList()));
      }
      // perform and
      return result.get(0);
    }

  }

  private List<ParsedUrl> createExecutableUrl(Map<String, List<IBaseResource>> idMap, ParsedUrl url, ResourceRegistry rr) {
    List<ParsedUrl> retVal = new ArrayList<>();
    String resource = url.getResource();
    if (url.getPlaceholder() != null) {
      if (idMap.get(url.getPlaceholder().getKey()) != null) {
        List<IBaseResource> list = idMap.get(url.getPlaceholder().getKey());
        List<String> identifiers = getIdentifiersFromResources(list, url.getPlaceholder(), this.getCtx(), this.s2f);
        int batch = rr.getMaxOr4Resource(resource);
        int counter = 0;
        List<String> idBatch = new ArrayList<>();
        for (String identifier: identifiers){        
          if (counter<batch){
            idBatch.add(identifier);
          } else {
            counter = 0;
            retVal.add(new ParsedUrl(resource, url.getKey(), StringUtils.join(idBatch,",")));
            idBatch.clear();
            idBatch.add(identifier);
          }
          counter++;
        }
        if (!idBatch.isEmpty()){
          retVal.add(new ParsedUrl(resource, url.getKey(), StringUtils.join(idBatch,",")));
        }
      }
    } else {
      retVal.add(url);
    }
    return retVal;
  }

  private static List<String> getIdentifiersFromResources(List<IBaseResource> list,
      DefaultMapEntry<String, List<String>> placeholder, FhirContext ctx, SearchParam2FhirPathRegistry s2f) {
    final List<IBase> toProcess = new ArrayList<>(list);
    final List<IBase> nextRound = new ArrayList<>();

    placeholder.getValue().forEach(searchParam -> {

      toProcess.forEach(inputResource -> {
        String resourceName = inputResource.getClass().getSimpleName();
        List<IBase> outputs;
        IFhirPath fhirPath = ctx.newFhirPath();
        String fp = s2f.getFhirPath(resourceName, searchParam);
        outputs = fhirPath.evaluate(inputResource, fp, IBase.class);
        nextRound.addAll(outputs);

      });

      toProcess.clear();
      toProcess.addAll(nextRound);
      nextRound.clear();

    });

    List<String> identifiers = toProcess.stream()
        .flatMap(x -> {
          Identifier id = (Identifier) x;
          Stream<String> out = null;
          if (id.getValue() == null) {
            out = Collections.<String>emptyList().stream();
          } else if (id.getSystem() == null) {
            out = Arrays.asList(id.getValue()).stream();
          } else {
            out = Arrays.<String>asList(id.getSystem() + "|" + id.getValue()).stream();
          }
          return out;
        }).distinct()

        .map(x -> {
          String encoded = null;
          try {
            encoded = URLEncoder.encode(x, "UTF-8");
          } catch (Exception e) {
          }
          ;
          return encoded;
        })

        .collect(Collectors.<String>toList());
    return identifiers;
  }

  @SuppressWarnings("unused")
  private List<IBaseResource> doSimpleMultiplex(HttpServletRequest theServletRequest, String tenantAndResource) {
    Set<String> keys = cr.getKeySet();
    Stream<String> stream = keys.parallelStream();
    List<IBaseResource> result = stream.flatMap(backendFhir -> {
      IGenericClient client = cr.getClient(backendFhir);
      String completeUrl;
      if (StringUtils.isNotBlank(theServletRequest.getQueryString())) {
        completeUrl = backendFhir + tenantAndResource + "?" + theServletRequest.getQueryString();
      } else {
        completeUrl = backendFhir + tenantAndResource;
      }
      ourLog.info("Client request Url: {}", completeUrl);
      return getResultsForURL(client, completeUrl).stream();
    }).collect(Collectors.<IBaseResource>toList());
    return result;
  }

  private static List<IBaseResource> getResultsForURL(IGenericClient client, String completeUrl) {
    Bundle bundle = client.search().byUrl(completeUrl).returnBundle(Bundle.class).execute();
    List<BundleLinkComponent> next = bundle.getLink().stream().filter(link -> link.getRelation().equals("next"))
        .collect(Collectors.toList());

    List<IBaseResource> own = bundle.getEntry().stream().filter(bec -> {
      return bec.getResource() instanceof IBaseResource;
    })
        .map(bec -> ((IBaseResource) bec.getResource())).collect(Collectors.toList());
    ourLog.info("Client request Url: {} #{}", completeUrl, own.size());
    List<IBaseResource> out = new ArrayList<>();
    out.addAll(own);
    if (!next.isEmpty()) {
      List<IBaseResource> other = getResultsForURL(client, next.get(0).getUrl());
      out.addAll(other);
    }
    return out;
  }

}
