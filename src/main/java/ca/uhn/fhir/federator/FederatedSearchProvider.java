package ca.uhn.fhir.federator;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FhirUrlParser.SContext;
import ca.uhn.fhir.federator.ast.AndNode;
import ca.uhn.fhir.federator.ast.IncludeNode;
import ca.uhn.fhir.federator.ast.Node;
import ca.uhn.fhir.federator.ast.NoopNode;
import ca.uhn.fhir.federator.ast.ParameterNode;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;

public class FederatedSearchProvider {
  static final org.slf4j.Logger ourLog =
      org.slf4j.LoggerFactory.getLogger(FederatedSearchProvider.class);

  private ClientRegistry cr;

  private ResourceRegistry rr;

  private FhirContext ctx;

  private SearchParam2FhirPathRegistry s2f;

  private boolean handlingStrict;

  public FhirContext getCtx() {
    return ctx;
  }

  public void setCtx(FhirContext ctx) {
    this.ctx = ctx;
  }

  public FederatedSearchProvider(
      ClientRegistry cr, ResourceRegistry rr, FhirContext ctx, SearchParam2FhirPathRegistry s2f) {
    this.cr = cr;
    this.rr = rr;
    this.ctx = ctx;
    this.s2f = s2f;
  }

  @Operation(
      name = "$doFederation",
      manualResponse = false,
      manualRequest = true,
      idempotent = true,
      global = true)
  public IBundleProvider manualInputAndOutput(
      HttpServletRequest theServletRequest, HttpServletResponse theServletResponse)
      throws IOException {
    String contentType = theServletRequest.getContentType();
    byte[] bytes = IOUtils.toByteArray(theServletRequest.getInputStream());
    String requestFullPath = StringUtils.defaultString(theServletRequest.getRequestURI());
    String servletPath = StringUtils.defaultString(theServletRequest.getServletPath());
    StringBuffer requestUrl = theServletRequest.getRequestURL();
    String tenantAndResource = StringUtils.defaultString(theServletRequest.getPathInfo());
    Enumeration<String> preferHeaders = theServletRequest.getHeaders("Prefer");
    for (String preferHeader = null;
        preferHeaders.hasMoreElements();
        preferHeader = preferHeaders.nextElement()) {
      if ("handling=strict".equals(preferHeader)) {
        this.handlingStrict = true;
      }
    }

    if (ourLog.isTraceEnabled()) {
      ourLog.trace("Request FullPath: {}", requestFullPath);
      ourLog.trace("Servlet Path: {}", servletPath);
      ourLog.trace("Request Url: {}", requestUrl);
    }

    ourLog.info("Received call with content type {} and {} bytes", contentType, bytes.length);
    IBundleProvider result = doWithASTQueryAnalysis(theServletRequest, tenantAndResource);

    return result;
  }

  private IBundleProvider doWithASTQueryAnalysis(
      HttpServletRequest theServletRequest, String tenantAndResource) throws IOException {
    // TODO split tenant and resource
    String toParse;
    if (StringUtils.isNotBlank(theServletRequest.getQueryString())) {
      toParse = tenantAndResource + "?" + theServletRequest.getQueryString();
    } else {
      toParse = tenantAndResource;
    }

    toParse = URLDecoder.decode(toParse, "UTF-8");

    CharStream inputCharStream = CharStreams.fromString(toParse.substring(1));
    TokenSource tokenSource = new FhirUrlLexer(inputCharStream);
    TokenStream inputTokenStream = new CommonTokenStream(tokenSource);
    FhirUrlParser parser = new FhirUrlParser(inputTokenStream);

    parser.addErrorListener(new ParserErrorListener());

    SContext context = parser.s();

    ourLog.info(TreeUtils.toPrettyTree(context, Arrays.asList(parser.getRuleNames())));

    ourLog.info(context.getText());

    FhirUrlAnalyser visitor = new FhirUrlAnalyser();

    context.accept(visitor);

    Node root = createAST(visitor);

    return root.execute();
  }

  private Node createAST(FhirUrlAnalyser visitor) {

    List<Node> perParameter =
        visitor.getAndParameters().stream()
            .map(httpParam -> createPartialUrls(handlingStrict, false, httpParam, visitor, s2f))
            .map(partialUrls -> new ParameterNode(partialUrls, rr, cr, ctx, s2f))
            .collect(Collectors.toList());

    AndNode and = new AndNode(perParameter);

    List<ParameterNode> perIncludeParameter =
        visitor.getIncludeParameters().stream()
            .map(httpParam -> createPartialUrls(handlingStrict, true, httpParam, visitor, s2f))
            .map(partialUrls -> new ParameterNode(partialUrls, rr, cr, ctx, s2f))
            .collect(Collectors.toList());

    List<Node> chain = new ArrayList<>();
    chain.add(and);
    chain.addAll(perIncludeParameter);
    Node include =
        chain.stream()
            .reduce((a, b) -> new IncludeNode(a, (ParameterNode) b))
            .orElse(NoopNode.EMPTY);

    return include;
  }

  private List<ParsedUrl> createPartialUrls(
      boolean handlingStrict,
      boolean dependent,
      ParserRuleContext httpParam,
      FhirUrlAnalyser visitor,
      SearchParam2FhirPathRegistry s2f) {
    return visitor.getResourcesForHttpParam(dependent, httpParam).stream()
        .map(resourceInParam -> new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
        .flatMap(
            opt ->
                opt.isPresent()
                    ? Arrays.<ParsedUrl>asList(opt.get()).stream()
                    : Stream.<ParsedUrl>empty())
        .map(parsedUrl -> validateKey(parsedUrl, handlingStrict, s2f))
        .collect(Collectors.toList());
  }

  private ParsedUrl validateKey(
      ParsedUrl parsedUrl, boolean handlingStrict, SearchParam2FhirPathRegistry s2f) {
    boolean ok = CollectionUtils.isEmpty(parsedUrl.getKey());
    if (!ok) {
      ok = s2f.searchParamExists(parsedUrl.getResource(), parsedUrl.getKey().get(0));
    }
    if (!ok && handlingStrict) {
      throw new InvalidRequestException(
          String.format(
              "SearchParam {} does not exist for resource {}",
              parsedUrl.getKey().get(0),
              parsedUrl.getResource()));
    }
    if (!ok && !handlingStrict) {
      return new ParsedUrl(parsedUrl.getResource());
    } else {
      return parsedUrl;
    }
  }
}
