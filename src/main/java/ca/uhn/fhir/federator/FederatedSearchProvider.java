package ca.uhn.fhir.federator;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FhirUrlParser.SContext;
import ca.uhn.fhir.federator.ast.AndNode;
import ca.uhn.fhir.federator.ast.IncludeNode;
import ca.uhn.fhir.federator.ast.Node;
import ca.uhn.fhir.federator.ast.ParameterNode;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.RequiredParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.param.TokenParam;

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
    IBundleProvider result = doWithASTQueryAnalysis(theServletRequest, tenantAndResource);

    return result;

  }

  private IBundleProvider doWithASTQueryAnalysis(HttpServletRequest theServletRequest, String tenantAndResource)
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

    Node root = createAST(visitor);

    return root.execute();
  }

  private Node createAST(FhirUrlAnalyser visitor) {

    List<Node> perParameter = visitor.getAndParameters()
        .stream()
        .map(httpParam -> createPartialUrls(httpParam, visitor))
        .map(partialUrls -> new ParameterNode(partialUrls, rr, cr, ctx, s2f))
        .collect(Collectors.toList());

    AndNode and = new AndNode(perParameter);

    List<ParameterNode> perIncludeParameter = visitor.getIncludeParameters()
        .stream()
        .map(httpParam -> createPartialUrls(httpParam, visitor))
        .map(partialUrls -> new ParameterNode(partialUrls, rr, cr, ctx, s2f))
        .collect(Collectors.toList());

    IncludeNode include = new IncludeNode(and, perIncludeParameter);

    return include;

  }

  private List<ParsedUrl> createPartialUrls(ParserRuleContext httpParam, FhirUrlAnalyser visitor) {
    return visitor.getResourcesForHttpParam(httpParam).stream()
        .map(resourceInParam -> new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
        .flatMap(opt -> opt.isPresent() ? Arrays.<ParsedUrl>asList(opt.get()).stream() : Stream.<ParsedUrl>empty())
        .collect(Collectors.toList());
  }
}
