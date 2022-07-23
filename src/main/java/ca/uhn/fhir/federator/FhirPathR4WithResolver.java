package ca.uhn.fhir.federator;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.r4.hapi.ctx.HapiWorkerContext;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.utils.FHIRPathEngine;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.fhirpath.FhirPathExecutionException;
import ca.uhn.fhir.fhirpath.IFhirPath;

public class FhirPathR4WithResolver implements IFhirPath {

  private final FHIRPathEngine myEngine;

  public FhirPathR4WithResolver(FhirContext theCtx, ClientRegistry cr, ResourceRegistry rr) {
    IValidationSupport validationSupport = theCtx.getValidationSupport();
    myEngine = new FHIRPathEngine(new HapiWorkerContext(theCtx, validationSupport));
    myEngine.setHostServices(
        new EvaluationContextWithResolver(myEngine.getHostServices(), theCtx, cr, rr));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T extends IBase> List<T> evaluate(
      IBase theInput, String thePath, Class<T> theReturnType) {
    List<Base> result;
    try {
      result = myEngine.evaluate((Base) theInput, thePath);
    } catch (FHIRException e) {
      throw new FhirPathExecutionException(e);
    }

    for (Base next : result) {
      if (!theReturnType.isAssignableFrom(next.getClass())) {
        throw new FhirPathExecutionException(
            "FluentPath expression \""
                + thePath
                + "\" returned unexpected type "
                + next.getClass().getSimpleName()
                + " - Expected "
                + theReturnType.getName());
      }
    }

    return (List<T>) result;
  }

  @Override
  public <T extends IBase> Optional<T> evaluateFirst(
      IBase theInput, String thePath, Class<T> theReturnType) {
    return evaluate(theInput, thePath, theReturnType).stream().findFirst();
  }

  @Override
  public void parse(String theExpression) {
    myEngine.parse(theExpression);
  }
}
