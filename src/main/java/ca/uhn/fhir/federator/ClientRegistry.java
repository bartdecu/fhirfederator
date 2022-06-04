package ca.uhn.fhir.federator;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class ClientRegistry {
    
    private Map<String,IGenericClient> map = new LinkedHashMap<>();

    public ClientRegistry(List<String> bases, FhirContext ctx){

        bases.stream().forEach(base -> map.put(base,ctx.newRestfulGenericClient(base) ));

    }

    IGenericClient getClient(String base){
        return map.get(base);
    }

    Collection<IGenericClient> getAll(){
        return map.values();
    }

    Set<String> getKeySet(){
        return map.keySet();
    }
     


    
}
