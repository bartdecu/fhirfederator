package ca.uhn.fhir.federator;

import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.BooleanType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FederatorProperties.ServerResourceConfig;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.annotation.Update;

public class ResourceConfigEvaluator {
  private static final org.slf4j.Logger ourLog =
      org.slf4j.LoggerFactory.getLogger(ResourceConfigEvaluator.class);

  private Class<?> action;
  private ServerResourceConfig config;
  private FhirContext ctx;
  private IBase resource;

  public ResourceConfigEvaluator(
      FhirContext ctx, Class<?> action, IBaseResource resource, ServerResourceConfig config) {
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
    } else if (Delete.class.equals(action)) {
      toEval = config.getDelete();
    }  else {
      return (Boolean) null;
    }

    if (toEval == null) {
      return true;
    } else if (Boolean.TRUE.toString().equals(toEval) || Boolean.FALSE.toString().equals(toEval)) {
      return Boolean.parseBoolean(toEval);
    } else if(Delete.class.equals(action)) {
      //we do not have a resource to evaluate
      //so if there is something else than true/false, this is an error
      ourLog.error("Unexpected content for delete property: {} {}", config.getServer(), toEval);
      return false;
    } else {
      try {
        IFhirPath fp = ctx.newFhirPath();
        Optional<BooleanType> ev = fp.evaluateFirst(resource, toEval, BooleanType.class);
        if (ev.isPresent() && ev.get().isBooleanPrimitive()) {
          BooleanType t = ev.get();
          return t.booleanValue();
        }
      } catch (RuntimeException e) {
        ourLog.error("Unexpected content for fhirpath {} {} {}", toEval, e.getMessage(), e);
        return false;
      }
      return false;
    }
  }
}
