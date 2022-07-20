package ca.uhn.fhir.federator;

import static ca.uhn.fhir.federator.TreeUtils.toPrettyTree;
import static java.nio.charset.StandardCharsets.UTF_8;

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
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;
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

public class FederatedSearchProvider {
  static final org.slf4j.Logger ourLog =
      org.slf4j.LoggerFactory.getLogger(FederatedSearchProvider.class);

  private final ClientRegistry cr;

  private final ResourceRegistry rr;

  private FhirContext ctx;

  private final SearchParam2FhirPathRegistry s2f;

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

  @Operation(name = "$doFederation", manualRequest = true, idempotent = true, global = true)
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
    // TODO split tenant and resource
    String toParse;
    if (StringUtils.isNotBlank(theServletRequest.getQueryString())) {
      toParse = tenantAndResource + "?" + theServletRequest.getQueryString();
    } else {
      toParse = tenantAndResource;
    }

    return searchWithAstQueryAnalysis(toParse.substring(1));
  }

  public IBundleProvider searchWithAstQueryAnalysis(String toParse) {
    toParse = URLDecoder.decode(toParse, UTF_8);

    CharStream inputCharStream = CharStreams.fromString(toParse);
    TokenSource tokenSource = new FhirUrlLexer(inputCharStream);
    TokenStream inputTokenStream = new CommonTokenStream(tokenSource);
    FhirUrlParser parser = new FhirUrlParser(inputTokenStream);

    parser.addErrorListener(new ParserErrorListener());

    SContext context = parser.s();

    ourLog.info(toPrettyTree(context, Arrays.asList(parser.getRuleNames())));

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

    AndNode and = new AndNode(rr, perParameter);

    List<ParameterNode> perIncludeParameter =
        visitor.getIncludeParameters().stream()
            .map(httpParam -> createPartialUrls(handlingStrict, true, httpParam, visitor, s2f))
            .map(partialUrls -> new ParameterNode(partialUrls, rr, cr, ctx, s2f))
            .collect(Collectors.toList());

    List<Node> chain = new ArrayList<>();
    chain.add(and);
    chain.addAll(perIncludeParameter);

    return chain.stream()
        .reduce((a, b) -> new IncludeNode(a, (ParameterNode) b))
        .orElse(NoopNode.EMPTY);
  }

  private List<ParsedUrl> createPartialUrls(
      boolean handlingStrict,
      boolean dependent,
      ParserRuleContext httpParam,
      FhirUrlAnalyser visitor,
      SearchParam2FhirPathRegistry s2f) {
    return visitor.getResourcesForHttpParam(dependent, httpParam).stream()
        .map(resourceInParam -> new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
        .flatMap(opt -> opt.stream())
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
    } else if (!ok) {
      return new ParsedUrl(parsedUrl.getResource());
    } else {
      return parsedUrl;
    }
  }
}
