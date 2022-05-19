package ca.uhn.fhir.federator;

public class ResourceConfig {

    String server;
    Integer maxOr;
    public ResourceConfig(String server, Integer maxOr) {
        this.server = server;
        this.maxOr = maxOr;
    }
    
    public String getServer() {
        return server;
    }
    public Integer getMaxOr() {
        return maxOr;
    }
 
}
