package ca.uhn.fhir.federator;

import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Update;

public class ResourceConfigEvaluator {
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ResourceConfigEvaluator.class);

    private Class<?> action;
    private ResourceConfig config;
    private FhirContext ctx;
    private IBase resource;

    public ResourceConfigEvaluator(FhirContext ctx, Class<?> action, IBaseResource resource, ResourceConfig config) {
        this.action = action;
        this.resource = resource;
        this.config = config;
        this.ctx = ctx;
    }

    public Boolean execute() {
        String toEval;
        if (Read.class.equals(action)) {
            toEval = config.getRead();
        } else if (Create.class.equals(action)) {
            toEval = config.getCreate();
        } else if (Update.class.equals(action)) {
            toEval = config.getUpdate();
        } else {
            return (Boolean) null;
        }

        if (toEval == null) {
            return true;
        } else if (Boolean.TRUE.toString().equals(toEval) || Boolean.FALSE.toString().equals(toEval)) {
            return Boolean.parseBoolean(toEval);
        } else {
            try {
                IFhirPath fp = ctx.newFhirPath();
                Optional<BooleanType> ev = fp.evaluateFirst(resource, toEval, BooleanType.class);
                if (ev.isPresent() && ev.get().isBooleanPrimitive()) {
                    BooleanType t = ev.get();
                    return t.booleanValue();
                }
            } catch (RuntimeException e) {
                ourLog.error(e.getMessage(), e);
                return false;
            }
            return false;

        }

    }

}
