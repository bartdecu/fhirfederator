package ca.uhn.fhir.federator;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.exceptions.PathEngineException;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.TypeDetails;
import org.hl7.fhir.r4.model.ValueSet;
import org.hl7.fhir.r4.utils.FHIRPathEngine.IEvaluationContext;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.UrlUtil;
import ca.uhn.fhir.util.UrlUtil.UrlParts;

public class EvaluationContextWithResolver implements IEvaluationContext {
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory
            .getLogger(EvaluationContextWithResolver.class);
    private IEvaluationContext delegate;
    private FhirContext ctx;
    private ClientRegistry cr;
    private ResourceRegistry rr;

    public EvaluationContextWithResolver(IEvaluationContext delegate, FhirContext ctx, ClientRegistry cr,
            ResourceRegistry rr) {
        this.delegate = delegate;
        this.ctx = ctx;
        this.cr = cr;
        this.rr = rr;
    }

    @Override
    public Base resolveConstant(Object appContext, String name, boolean beforeContext) throws PathEngineException {
        return delegate.resolveConstant(appContext, name, beforeContext);
    }

    @Override
    public TypeDetails resolveConstantType(Object appContext, String name) throws PathEngineException {
        return delegate.resolveConstantType(appContext, name);
    }

    @Override
    public boolean log(String argument, List<Base> focus) {
        return delegate.log(argument, focus);
    }

    @Override
    public FunctionDetails resolveFunction(String functionName) {
        return delegate.resolveFunction(functionName);
    }

    @Override
    public TypeDetails checkFunction(Object appContext, String functionName, List<TypeDetails> parameters)
            throws PathEngineException {
        return delegate.checkFunction(appContext, functionName, parameters);
    }

    @Override
    public List<Base> executeFunction(Object appContext, List<Base> focus, String functionName,
            List<List<Base>> parameters) {
        return delegate.executeFunction(appContext, focus, functionName, parameters);
    }

    @Override
    public Base resolveReference(Object appContext, String url) throws FHIRException {
        UrlParts p = UrlUtil.parseUrl(url);
        String resource = p.getResourceType();
        int till = url.lastIndexOf(resource, url.length());
        List<String> servers;
        if (till < 1) {
             servers = rr.getServer4Resource(resource).stream().map(x -> x.getServer()).collect(Collectors.toList());
        } else {
            servers = Arrays.asList(url.substring(0, till));
        }
        List<Base> retVal = servers.stream().flatMap(server ->{
            try {
                return Arrays.asList((Base) ctx.newRestfulGenericClient(server).read().resource(resource).withUrl(url)
                        .execute()).stream();
            } catch (Exception e) {
                ourLog.error("Reference not resolved: {}", e.getMessage());
                return Stream.<Base>empty();
            }
        }).collect(Collectors.toList());

        return retVal.isEmpty()?null:retVal.get(0);
        
    }

    @Override
    public boolean conformsToProfile(Object appContext, Base item, String url) throws FHIRException {
        return delegate.conformsToProfile(appContext, item, url);
    }

    @Override
    public ValueSet resolveValueSet(Object appContext, String url) {
        return delegate.resolveValueSet(appContext, url);
    }

}