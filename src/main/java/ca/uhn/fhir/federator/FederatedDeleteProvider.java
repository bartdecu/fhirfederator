package ca.uhn.fhir.federator;

import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class FederatedDeleteProvider extends FederatedProvider {

  /**
   * Constructor
   *
   * @param rr
   * @param cr
   */
  public FederatedDeleteProvider(
      FhirContext ctx,
      ClientRegistry cr,
      ResourceRegistry rr,
      SearchParam2FhirPathRegistry s2f,
      Class<? extends IBaseResource> br) {
    super(ctx, cr, rr, br, s2f);

  }

  @Delete
  public MethodOutcome delete(@IdParam IdType theId, @ConditionalUrlParam String theConditional) {

    if (theId != null) {

      Optional<IGenericClient> client = getClient(Delete.class);

      if (!client.isPresent()) {
        throw new UnprocessableEntityException(
            Msg.code(636) + "No memberserver available for the deletion of this resource");
      }

      IdType newId =
          new IdType(
              client.get().getServerBase(),
              theId.getResourceType(),
              theId.getId(),
              theId.getVersionIdPart());
      return action(null, client.get(), newId);
    } else {

      return doConditionalAction(Delete.class, null, theConditional);

      
    }
  }

  @Override
  protected MethodOutcome action(IBaseResource resource, IGenericClient client, IdType newId) {
    return  client.delete().resourceById(newId).execute();
  }
}
