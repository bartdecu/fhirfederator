package ca.uhn.fhir.federator;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.annotation.ConditionalUrlParam;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public class FederatedDeleteProvider extends FederatedProvider {

  private FederatedSearchProvider fsp;

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
    super(ctx, cr, rr, br);
    this.fsp = new FederatedSearchProvider(cr, rr, ctx, s2f);
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
      return client.get().delete().resourceById(newId).execute();
    } else {

      IBundleProvider result = fsp.searchWithAstQueryAnalysis(theConditional);

      List<IIdType> updatableResources =
          result.getAllResources().stream()
              .map(x -> x.getIdElement())
              .filter(x -> getResourceType().getSimpleName().equals(x.getResourceType()))
              .toList();

      List<MethodOutcome> retVal =
          updatableResources.stream()
              .map(
                  x -> {
                    String id = x.getIdPart();
                    String versionString = x.getVersionIdPart();
                    Optional<IGenericClient> client;

                    if (x.hasBaseUrl()) {
                      client =
                          Optional.ofNullable(getCtx().newRestfulGenericClient(x.getBaseUrl()));
                    } else {
                      client = getClient(Delete.class);
                    }
                    if (!client.isPresent()) {
                      throw new UnprocessableEntityException(
                          Msg.code(636)
                              + "No memberserver available for the update of this resource");
                    }

                    IdType newId =
                        new IdType(
                            client.get().getServerBase(), x.getResourceType(), id, versionString);

                    return client.get().delete().resourceById(newId).execute();
                  })
              .collect(Collectors.toList());

      // TODO: improve - add all deleted resources as headerValues?
      return retVal.get(0);
    }
  }
}
