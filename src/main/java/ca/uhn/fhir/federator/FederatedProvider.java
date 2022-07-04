package ca.uhn.fhir.federator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.map.SingletonMap;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FederatorProperties.ServerResourceConfig;
import ca.uhn.fhir.i18n.Msg;
import ca.uhn.fhir.rest.annotation.Delete;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;

public abstract class FederatedProvider implements IResourceProvider {
  private static final org.slf4j.Logger ourLog =
      org.slf4j.LoggerFactory.getLogger(FederatedProvider.class);
  public static final String RFC_HTTP_ERROR_CODE = "#section-10";
  public static final String RFC2616 = "https://datatracker.ietf.org/doc/html/rfc2616";
  private Class<? extends IBaseResource> br;
  private ClientRegistry cr;
  private ResourceRegistry rr;
  private FhirContext ctx;
  private SearchParam2FhirPathRegistry s2f;
  private FederatedSearchProvider fsp;

  /**
   * Constructor
   *
   * @param ctx
   * @param br
   * @param rr
   * @param cr
   */
  protected FederatedProvider(
      FhirContext ctx,
      ClientRegistry cr,
      ResourceRegistry rr,
      Class<? extends IBaseResource> br,
      SearchParam2FhirPathRegistry s2f) {
    this.ctx = ctx;
    this.br = br;
    this.cr = cr;
    this.rr = rr;
    this.s2f = s2f;

    this.fsp = s2f == null ? null : new FederatedSearchProvider(cr, rr, ctx, s2f);
  }

  public FhirContext getCtx() {
    return ctx;
  }

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return this.br;
  }

  protected Optional<IGenericClient> getClient(Class<?> action, IBaseResource resource) {
    List<ServerResourceConfig> servers = rr.getServer4Resource(br.getSimpleName()).getLocations();

    Optional<ServerResourceConfig> preferred =
        servers.stream().filter(x -> evaluate(action, resource, x)).findFirst();
    if (!preferred.isPresent()) {
      return Optional.empty();
    }
    return Optional.ofNullable(cr.getClient(preferred.get().getServer()));
  }

  private Boolean evaluate(Class<?> action, IBaseResource resource, ServerResourceConfig config) {

    return new ResourceConfigEvaluator(ctx, action, resource, config).execute();
  }

  protected Optional<IGenericClient> getClient(Class<Delete> delete) {
    return getClient(delete, null);
  }

  protected abstract MethodOutcome action(
      IBaseResource resource, IGenericClient client, IdType newId);

  private Stream<? extends MethodOutcome> performAction(
      Class<?> action, IBaseResource resource, String type, IIdType x) {
    String id = x.getIdPart();
    String versionString = x.getVersionIdPart();
    Optional<IGenericClient> client;
    Stream<MethodOutcome> outcomeStream = null;
    if (x.hasBaseUrl()) {
      client = Optional.ofNullable(getCtx().newRestfulGenericClient(x.getBaseUrl()));
    } else {
      client = getClient(action, resource);
    }
    if (!client.isPresent()) {
      throw new UnprocessableEntityException(
          Msg.code(636) + "No memberserver available for the update of this resource");
    }
    IdType newId = new IdType(client.get().getServerBase(), type, id, versionString);
    try {
      MethodOutcome outcome = action(resource, client.get(), newId);
      outcomeStream = Arrays.<MethodOutcome>asList(outcome).stream();

    } catch (BaseServerResponseException e) {
      ourLog.error("{}", e.getMessage());
      int status = e.getStatusCode();
      MethodOutcome outcome =
          new MethodOutcome()
              .setId(new IdType(RFC2616, RFC_HTTP_ERROR_CODE, Integer.toString(status), null));
      outcome.setResponseHeaders(
          new SingletonMap<String, List<String>>("Id", Arrays.asList(newId.getValue())));
      outcomeStream = Arrays.<MethodOutcome>asList(outcome).stream();
    }
    return outcomeStream;
  }

  protected MethodOutcome doConditionalAction(
      Class<?> action, IBaseResource resource, String theConditional) {
    if (fsp == null) {
      throw new NotImplementedException();
    }

    IBundleProvider result = fsp.searchWithAstQueryAnalysis(theConditional);

    String type = getResourceType().getSimpleName();
    List<IIdType> updatableResources =
        result.getAllResources().stream()
            .map(x -> x.getIdElement())
            .filter(x -> type.equals(x.getResourceType()))
            .collect(Collectors.toList());

    List<MethodOutcome> retVal =
        updatableResources.stream()
            .flatMap(x -> performAction(action, resource, type, x))
            .collect(Collectors.toList());

    Optional<MethodOutcome> rv =
        retVal.stream()
            .filter(x -> !RFC_HTTP_ERROR_CODE.equals(x.getId().getResourceType()))
            .findFirst();

    if (rv.isPresent()) {
      return rv.get();
    }

    rv =
        retVal.stream()
            .filter(x -> RFC_HTTP_ERROR_CODE.equals(x.getId().getResourceType()))
            .findFirst();

    if (rv.isPresent()) {
      throw BaseServerResponseException.newInstance(
          rv.get().getId().getIdPartAsLong().intValue(),
          StringUtils.join(rv.get().getResponseHeaders().get("Id"), ";"));
    } else {
      throw new ResourceNotFoundException(theConditional);
    }
  }
}
