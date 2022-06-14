package ca.uhn.fhir.federator;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.TokenSource;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
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

public class FederatedSearchProvider {
  private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(FederatedSearchProvider.class);

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

  @Operation(name = "$doFederation", manualResponse = false, manualRequest = true, idempotent = true, global=true)
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

    toParse = URLDecoder.decode(toParse, "UTF-8");

    CharStream inputCharStream = CharStreams.fromString(toParse.substring(1));
    TokenSource tokenSource = new FhirUrlLexer(inputCharStream);
    TokenStream inputTokenStream = new CommonTokenStream(tokenSource);
    FhirUrlParser parser = new FhirUrlParser(inputTokenStream);

    parser.addErrorListener(new ANTLRErrorListener() {

      @Override
      public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
          String msg, RecognitionException e) {
        ourLog.error("Parsing error {} {} {} {} {}", offendingSymbol, line, charPositionInLine, msg, e.getMessage());
        
      }

      @Override
      public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact,
          BitSet ambigAlts, ATNConfigSet configs) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex,
          BitSet conflictingAlts, ATNConfigSet configs) {
        // TODO Auto-generated method stub
        
      }

      @Override
      public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction,
          ATNConfigSet configs) {
        // TODO Auto-generated method stub
        
      }
      
    });

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
        .map(httpParam -> createPartialUrls(false, httpParam, visitor))
        .map(partialUrls -> new ParameterNode(partialUrls, rr, cr, ctx, s2f))
        .collect(Collectors.toList());

    AndNode and = new AndNode(perParameter);

    List<ParameterNode> perIncludeParameter = visitor.getIncludeParameters()
        .stream()
        .map(httpParam -> createPartialUrls(true, httpParam, visitor))
        .map(partialUrls -> new ParameterNode(partialUrls, rr, cr, ctx, s2f))
        .collect(Collectors.toList());

    List<Node> chain = new ArrayList<>();
    chain.add(and);
    chain.addAll(perIncludeParameter);
    Node include = chain.stream().reduce((a, b) -> new IncludeNode(a, (ParameterNode) b)).orElse(NoopNode.EMPTY);

    return include;

  }

  private List<ParsedUrl> createPartialUrls(boolean dependent,ParserRuleContext httpParam, FhirUrlAnalyser visitor) {
    return visitor.getResourcesForHttpParam(dependent, httpParam).stream()
        .map(resourceInParam -> new ParsedUrlCreator(resourceInParam, httpParam).createUrl())
        .flatMap(opt -> opt.isPresent() ? Arrays.<ParsedUrl>asList(opt.get()).stream() : Stream.<ParsedUrl>empty())
        .collect(Collectors.toList());
  }
}
