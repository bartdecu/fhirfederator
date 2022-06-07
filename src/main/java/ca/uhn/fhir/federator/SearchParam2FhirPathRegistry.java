package ca.uhn.fhir.federator;

import java.util.HashMap;
import java.util.Map;

public class SearchParam2FhirPathRegistry {
    Map<String, String> searchParam2FhirPath = new HashMap<>();
    public SearchParam2FhirPathRegistry(){
        searchParam2FhirPath.put("resolve()","resolve()");

    }

    void put(String searchParam, String fhirPath){
        searchParam2FhirPath.put(searchParam, fhirPath);
    }

    String getFhirPath(String resource, String searchParam){

        String retVal = searchParam2FhirPath.get(resource + "." +searchParam);
        if (retVal == null){
            retVal = searchParam2FhirPath.get(searchParam);
        }
        if (retVal == null){
            retVal = (resource + "." +searchParam);
        }
        return retVal;
    }
}
