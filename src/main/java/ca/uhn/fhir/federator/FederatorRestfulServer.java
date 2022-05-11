package ca.uhn.fhir.federator;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.yaml.snakeyaml.Yaml;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

@WebServlet("/*")
public class FederatorRestfulServer extends RestfulServer {

	@Override
	@SuppressWarnings("unchecked")
	protected void initialize() throws ServletException {

		Map<String, Object> config = getConfiguration();

		Object clients = config.get("clients");
		if(clients == null){
			throw new RuntimeException("No clients in federator.yaml");
		}
		List<String> clientUrls = new ArrayList<>();
		for (Map<String,Object> client: (List<Map<String,Object>>) clients){
			 clientUrls.add((String)client.get("url"));
		}

		Map<String,Object> resources = (Map<String,Object>) config.get("resources");
		if(resources == null){
			throw new RuntimeException("No resources in federator.yaml");
		}
		List<Map<String,Object>> defaults = (List<Map<String,Object>>) resources.get("default");
		if(defaults == null){
			throw new RuntimeException("No resources.default in federator.yaml");
		}
		List<String> defaultUrls = new ArrayList<>();
		for (Map<String, Object> deFault : defaults){
			defaultUrls.add((String)deFault.get("url"));

		}
		List<DefaultKeyValue<String,List<String>>> resourceUrls = new ArrayList<>();
		Map<String,Object> otherResources = (Map<String,Object>) resources.get("other");
		if (otherResources != null){
			
			for ( String otherResource:otherResources.keySet()){
				List<Map<String, Object>> resourceObjects =(List<Map<String, Object>>) otherResources.get(otherResource);
				List<String> resourceLocs = new ArrayList<>();
				for (Map<String, Object> resourceObject:resourceObjects){
					resourceLocs.add((String)resourceObject.get("url"));
				}
				resourceUrls.add(new DefaultKeyValue<>(otherResource,resourceLocs));
			}
		}
		Map<String,Object> searchParams = (Map<String,Object>) config.get("searchParams");
		if(searchParams == null){
			throw new RuntimeException("No searchParams in federator.yaml");
		}
		List<DefaultKeyValue<String,String>> searchParamsFhirPath = new ArrayList<>();
		for ( String searchParam:searchParams.keySet()){
				searchParamsFhirPath.add(new DefaultKeyValue<>(searchParam,(String)searchParams.get(searchParam)));
		}

		
       
		// Create a context for the appropriate version
		setFhirContext(FhirContext.forR4());
		ClientRegistry cr = new ClientRegistry(clientUrls);
		ResourceRegistry rr = new ResourceRegistry(defaultUrls);
		for (DefaultKeyValue<String, List<String>> resourceUrl:resourceUrls){
			for (String url:resourceUrl.getValue()){
				rr.putServer4Resource(resourceUrl.getKey(), url);
			}

		}
		SearchParam2FhirPathRegistry s2f = new SearchParam2FhirPathRegistry();
    	for (DefaultKeyValue<String, String> searchParamFhirPath:searchParamsFhirPath){			
				s2f.put(searchParamFhirPath.getKey(), searchParamFhirPath.getValue());
		}
		registerProvider(new FederatedSearchProvider(cr,rr, this.getFhirContext(),s2f));
		registerInterceptor(new FederatorInterceptor());
		registerInterceptor(new ResponseHighlighterInterceptor());
	}

	private Map<String, Object> getConfiguration() {
		Yaml yaml = new Yaml();
		InputStream inputStream = this.getClass()
				.getClassLoader()
				.getResourceAsStream("federator.yaml");
		Map<String, Object> obj = yaml.load(inputStream);
		return obj;
	}
}
