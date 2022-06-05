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

    private List<ParsedUrl> parsedUrls;
    private ParameterExecutor parameterExecutor;
    private ResourceRegistry rr;
    private ClientRegistry cr;
    private FhirContext ctx;
    private SearchParam2FhirPathRegistry s2f;

    public ParameterNode(List<ParsedUrl> parsedUrls, ResourceRegistry rr,
            ClientRegistry cr, FhirContext ctx, SearchParam2FhirPathRegistry s2f) {
        this.parsedUrls = parsedUrls;
        this.rr = rr;
        this.cr = cr;
        this.ctx = ctx;
        this.s2f = s2f;
    }

    @Override
    public IBundleProvider execute() {
        parameterExecutor = new ParameterExecutor(parsedUrls, rr, cr, ctx, s2f);
        List<IBaseResource> parameterResources = parameterExecutor.execute();
        return new SimpleBundleProvider(parameterResources);
    }

    public IBundleProvider executeWithReference(IBundleProvider reference) {
        parameterExecutor = new ParameterExecutor(parsedUrls, rr, cr, ctx, s2f);
        List<IBaseResource> resources = reference.getAllResources();
        Map<String, List<IBaseResource>> resourceCachePerParameter = resources.stream()
                .collect(Collectors.groupingBy(x -> x.getClass().getSimpleName(), HashMap::new, Collectors.toList()));
        parameterExecutor.setCachedResources(resourceCachePerParameter);
        List<IBaseResource> parameterResources = parameterExecutor.execute();
        return new SimpleBundleProvider(parameterResources);
    }

}
