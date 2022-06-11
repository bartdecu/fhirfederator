package ca.uhn.fhir.federator;

import java.util.List;

import org.hl7.fhir.r4.model.CapabilityStatement;

import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;
import ca.uhn.fhir.rest.annotation.Metadata;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class CapabilityStatementProvider {

    private ResourceRegistry rr;
    private ClientRegistry cr;

    public CapabilityStatementProvider(ClientRegistry cr, ResourceRegistry rr){
        this.cr = cr;
        this.rr = rr;
    }

    @Metadata
    public CapabilityStatement getServerMetadata() {
        List<ResourceConfig> servers = rr.getServer4Resource("metadata");
        //get first metadata
        ResourceConfig server = servers.get(0);
        IGenericClient client = cr.getClient(server.getServer());

        return client.capabilities().ofType(CapabilityStatement.class).execute();
    }
  
  }