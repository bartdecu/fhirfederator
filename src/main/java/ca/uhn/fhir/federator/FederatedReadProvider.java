package ca.uhn.fhir.federator;

import java.util.List;
import java.util.Optional;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Read;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;

public class FederatedReadProvider implements IResourceProvider {

   
 
    private Class<? extends IBaseResource> br;
    private ClientRegistry cr;
    private ResourceRegistry rr;

    /**
     * Constructor
     * @param rr
     * @param cr
     */
    public FederatedReadProvider(ClientRegistry cr, ResourceRegistry rr, Class<? extends IBaseResource> br) {
      this.br = br;
      this.cr = cr;
      this.rr = rr;
    }
 
    @Override
    public Class<? extends IBaseResource> getResourceType() {
       return this.br;
    }
 
    /**
     * Simple implementation of the "read" method
     */
    @Read(version=true)
    public IBaseResource read(@IdParam IdType theId) {
        List<ResourceConfig> servers = rr.getServer4Resource(br.getSimpleName());
        
        
            Optional<ResourceConfig> preferred = servers.stream().filter(x -> Boolean.TRUE.equals(x.getRead())).findAny();
            if (!preferred.isPresent()){
                preferred = servers.stream().findFirst();
            }

            if (!preferred.isPresent()){
                throw new ResourceNotFoundException(theId);
            }

            return cr.getClient(preferred.get().getServer()).read().resource(br).withId(theId).execute();
        
    }
 
 
 }
