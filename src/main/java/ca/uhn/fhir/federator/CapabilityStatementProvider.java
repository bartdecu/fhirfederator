package ca.uhn.fhir.federator;

import java.util.List;

import org.hl7.fhir.r4.model.CapabilityStatement;

import ca.uhn.fhir.federator.FederatorProperties.ServerResourceConfig;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class CapabilityStatementProvider {

  private final ResourceRegistry rr;
  private final ClientRegistry cr;

  public CapabilityStatementProvider(ClientRegistry cr, ResourceRegistry rr) {
    this.cr = cr;
    this.rr = rr;
  }

  @Metadata
  public CapabilityStatement getServerMetadata() {
    List<ServerResourceConfig> servers = rr.getServer4Resource("metadata").getLocations();
    // get first metadata
    ServerResourceConfig server = servers.get(0);
    IGenericClient client = cr.getClient(server.getServer());

    return client.capabilities().ofType(CapabilityStatement.class).execute();
  }
}
