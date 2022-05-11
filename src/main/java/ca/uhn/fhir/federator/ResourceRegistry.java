package ca.uhn.fhir.federator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceRegistry {
    
    private Map<String,List<String>> map = new LinkedHashMap<>();
    private List<String> defaultServers;

    public ResourceRegistry(List<String> defaults){
        this.defaultServers = defaults;
    }

    List<String> getServer4Resource(String resource){
        List<String> list = map.get(resource);
        if (list == null || list.isEmpty()){
            return defaultServers;
        } else {
            return list;
        }
    }


    void putServer4Resource(String resource, String server){
        List<String> list = map.get(resource);
        if (list == null){
            map.put(resource,new ArrayList<>());
        }
        list = map.get(resource);
        if (list.contains(server)){
            return;
        } else {
            list.add(server);
        }
    }     
}
