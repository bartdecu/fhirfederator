package ca.uhn.fhir.federator;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.IResourceProvider;

public abstract class FederatedProvider implements IResourceProvider {
  private Class<? extends IBaseResource> br;
  private ClientRegistry cr;
  private ResourceRegistry rr;
  private FhirContext ctx;

  /**
   * Constructor
   *
   * @param ctx
   * @param br
   * @param rr
   * @param cr
   */
  protected FederatedProvider(
      FhirContext ctx, ClientRegistry cr, ResourceRegistry rr, Class<? extends IBaseResource> br) {
    this.ctx = ctx;
    this.br = br;
    this.cr = cr;
    this.rr = rr;
  }

  @Override
  public Class<? extends IBaseResource> getResourceType() {
    return this.br;
  }

  protected Optional<IGenericClient> getClient(Class<?> action, IBaseResource resource) {
    List<ResourceConfig> servers = rr.getServer4Resource(br.getSimpleName());

    Optional<ResourceConfig> preferred =
        servers.stream().filter(x -> evaluate(action, resource, x)).findFirst();
    if (!preferred.isPresent()) {
      return Optional.empty();
    }
    return Optional.ofNullable(cr.getClient(preferred.get().getServer()));
  }

  private Boolean evaluate(Class<?> action, IBaseResource resource, ResourceConfig config) {

    return new ResourceConfigEvaluator(ctx, action, resource, config).execute();
  }
}
