package ca.uhn.fhir.federator.ast;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.ClientRegistry;
import ca.uhn.fhir.federator.ParameterExecutor;
import ca.uhn.fhir.federator.ParsedUrl;
import ca.uhn.fhir.federator.ResourceRegistry;
import ca.uhn.fhir.federator.SearchParam2FhirPathRegistry;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;

public class ParameterNode implements Node {

  private final List<ParsedUrl> parsedUrls;
  private final ResourceRegistry rr;
  private final ClientRegistry cr;
  private final FhirContext ctx;
  private final SearchParam2FhirPathRegistry s2f;
  private final boolean iterate;

  public ParameterNode(
      List<ParsedUrl> parsedUrls,
      ResourceRegistry rr,
      ClientRegistry cr,
      FhirContext ctx,
      SearchParam2FhirPathRegistry s2f) {
    this.parsedUrls = parsedUrls;
    this.rr = rr;
    this.cr = cr;
    this.ctx = ctx;
    this.s2f = s2f;
    this.iterate = parsedUrls.stream().anyMatch(ParsedUrl::isIterate);
  }

  @Override
  public IBundleProvider execute() {
    return executeWithReference(null);
  }

  public IBundleProvider executeWithReference(IBundleProvider reference) {
    ParameterExecutor parameterExecutor = new ParameterExecutor(parsedUrls, rr, cr, ctx, s2f);
    if (reference != null) {
      List<IBaseResource> resources = reference.getAllResources();
      Map<String, List<IBaseResource>> resourceCachePerParameter =
          resources.stream()
              .collect(
                  Collectors.groupingBy(
                      x -> x.getClass().getSimpleName(), HashMap::new, Collectors.toList()));
      parameterExecutor.setCachedResources(resourceCachePerParameter);
    }
    List<IBaseResource> parameterResources = parameterExecutor.execute();
    return new SimpleBundleProvider(parameterResources);
  }

  public boolean isIterate() {
    return iterate;
  }
}
