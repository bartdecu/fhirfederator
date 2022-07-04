package ca.uhn.fhir.federator;

import java.util.Optional;

import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

public class FederatedReadProvider extends FederatedProvider {

  public FederatedReadProvider(
      FhirContext ctx, ClientRegistry cr, ResourceRegistry rr, Class<? extends IBaseResource> br) {
    super(ctx, cr, rr, br, null);
  }

  /** Simple implementation of the "read" method */
  @Read(version = true)
  public IBaseResource read(@IdParam IdType theId) {

    Optional<IGenericClient> client = getClient(Read.class, null);

    if (!client.isPresent()) {
      throw new ResourceNotFoundException(theId);
    }

    return client.get().read().resource(getResourceType()).withId(theId).execute();
  }

  @Override
  protected MethodOutcome action(IBaseResource resource, IGenericClient client, IdType newId) {
    throw new NotImplementedException();
  }
}
