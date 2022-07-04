package ca.uhn.fhir.federator;

import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.ResourceParam;
import ca.uhn.fhir.rest.annotation.Update;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class FederatedUpdateProvider extends FederatedProvider {  

  private static final org.slf4j.Logger ourLog =
      org.slf4j.LoggerFactory.getLogger(FederatedUpdateProvider.class);


  /**
   * Constructor
   *
   * @param rr
   * @param cr
   */
  public FederatedUpdateProvider(
      FhirContext ctx,
      ClientRegistry cr,
      ResourceRegistry rr,
      SearchParam2FhirPathRegistry s2f,
      Class<? extends IBaseResource> br) {
    super(ctx, cr, rr, br, s2f);
    
  }

  @Update
  public MethodOutcome update(
      @ResourceParam IBaseResource resource,
      @IdParam IdType theId,
      @ConditionalUrlParam String theConditional) {

    if (theId != null) {

      Optional<IGenericClient> client = getClient(Update.class, resource);

      if (!client.isPresent()) {
        throw new UnprocessableEntityException(
            Msg.code(636) + "No memberserver available for the update of this resource");
      }
      IdType newId =
          new IdType(
              client.get().getServerBase(),
              theId.getResourceType(),
              theId.getId(),
              theId.getVersionIdPart());

      return action(resource, client.get(), newId);
    } else {

      return doConditionalAction(Update.class, resource, theConditional);
    }
  }

  

  



@Override
  protected MethodOutcome action(IBaseResource resource, IGenericClient client, IdType newId) {
    return client.update().resource(resource).withId(newId).execute();
  }

  
  
}
