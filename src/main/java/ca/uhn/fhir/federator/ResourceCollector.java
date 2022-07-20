package ca.uhn.fhir.federator;

import java.util.ArrayList;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBase;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;

public class ResourceCollector {
  private final List<IBase> input;
  private final List<String> searchParams;
  private final SearchParam2FhirPathRegistry s2f;
  private final ResourceRegistry rr;
  private final ClientRegistry cr;
  private final FhirContext ctx;

  public ResourceCollector(
      FhirContext ctx,
      ClientRegistry cr,
      ResourceRegistry rr,
      SearchParam2FhirPathRegistry s2f,
      List<IBase> input,
      List<String> searchParams) {
    this.ctx = ctx;
    this.cr = cr;
    this.rr = rr;
    this.s2f = s2f;
    this.input = input;
    this.searchParams = searchParams;
  }

  public List<IBase> execute() {
    final List<IBase> toProcess = new ArrayList<>(this.input);
    final List<IBase> nextRound = new ArrayList<>();

    this.searchParams.forEach(
        searchParam -> {
          toProcess.forEach(
              inputResource -> {
                String resourceName = inputResource.getClass().getSimpleName();
                List<IBase> outputs;
                // IFhirPath fhirPath = ctx.newFhirPath();
                IFhirPath fhirPath = new FhirPathR4WithResolver(ctx, cr, rr);
                String fp = s2f.getFhirPath(resourceName, searchParam);
                outputs = fhirPath.evaluate(inputResource, fp, IBase.class);
                nextRound.addAll(outputs);
              });

          toProcess.clear();
          toProcess.addAll(nextRound);
          nextRound.clear();
        });
    return toProcess;
  }
}
