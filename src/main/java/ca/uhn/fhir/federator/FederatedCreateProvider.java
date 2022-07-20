package ca.uhn.fhir.federator;

import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.annotation.Create;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class FederatedCreateProvider extends FederatedProvider {

  /**
   * Constructor
   *
   */
  public FederatedCreateProvider(
      FhirContext ctx, ClientRegistry cr, ResourceRegistry rr, Class<? extends IBaseResource> br) {
    super(ctx, cr, rr, br, null);
  }

  @Create
  public MethodOutcome createPatient(@ResourceParam IBaseResource resource) {

    Optional<IGenericClient> client = getClient(Create.class, resource);

    if (client.isEmpty()) {
      throw new UnprocessableEntityException(
          Msg.code(636) + "No memberserver available for the creation of this resource");
    }

    return action(resource, client.get(), null);
  }

  @Override
  protected MethodOutcome action(IBaseResource resource, IGenericClient client, IdType newId) {

    return client.create().resource(resource).execute();
  }
}
