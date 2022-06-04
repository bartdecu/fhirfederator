package ca.uhn.fhir.federator;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.apache.commons.collections4.keyvalue.DefaultKeyValue;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.SearchParameter;
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
		if (clients == null) {
			throw new RuntimeException("No clients in federator.yaml");
		}
		List<String> clientUrls = new ArrayList<>();
		for (Map<String, Object> client : (List<Map<String, Object>>) clients) {
			clientUrls.add((String) client.get("url"));
		}

		Object setup = config.get("setup");
		if (setup == null) {
			throw new RuntimeException("No setup in federator.yaml");
		}
		String setupUrl = (String) ((Map<String, Object>) setup).get("url");
		if (setupUrl == null) {
			throw new RuntimeException("No setupUrl in federator.yaml");
		}

		Map<String, Object> resources = (Map<String, Object>) config.get("resources");
		if (resources == null) {
			throw new RuntimeException("No resources in federator.yaml");
		}
		List<Map<String, Object>> defaults = (List<Map<String, Object>>) resources.get("default");
		if (defaults == null) {
			throw new RuntimeException("No resources.default in federator.yaml");
		}
		List<ResourceConfig> defaultUrls = new ArrayList<>();
		for (Map<String, Object> deFault : defaults) {
			defaultUrls.add(new ResourceConfig((String) deFault.get("url"), (Integer) deFault.get("maxOr")));

		}
		List<DefaultKeyValue<String, List<ResourceConfig>>> resourceUrls = new ArrayList<>();
		Map<String, Object> otherResources = (Map<String, Object>) resources.get("other");
		if (otherResources != null) {

			for (String otherResource : otherResources.keySet()) {
				List<Map<String, Object>> resourceObjects = (List<Map<String, Object>>) otherResources
						.get(otherResource);
				List<ResourceConfig> resourceLocs = new ArrayList<>();
				for (Map<String, Object> resourceObject : resourceObjects) {
					resourceLocs.add(new ResourceConfig((String) resourceObject.get("url"),
							(Integer) resourceObject.get("maxOr")));
				}
				resourceUrls.add(new DefaultKeyValue<>(otherResource, resourceLocs));
			}
		}
		/*
		 * Map<String, Object> searchParams = (Map<String, Object>)
		 * config.get("searchParams");
		 * if (searchParams == null) {
		 * throw new RuntimeException("No searchParams in federator.yaml");
		 * }
		 * List<DefaultKeyValue<String, String>> searchParamsFhirPath = new
		 * ArrayList<>();
		 * for (String searchParam : searchParams.keySet()) {
		 * searchParamsFhirPath.add(new DefaultKeyValue<>(searchParam, (String)
		 * searchParams.get(searchParam)));
		 * }
		 */

		// Create a context for the appropriate version
		setFhirContext(FhirContext.forR4());
		ClientRegistry cr = new ClientRegistry(clientUrls, this.getFhirContext());
		ResourceRegistry rr = new ResourceRegistry(defaultUrls);
		for (DefaultKeyValue<String, List<ResourceConfig>> resourceUrl : resourceUrls) {
			for (ResourceConfig resourceConfig : resourceUrl.getValue()) {
				rr.putServer4Resource(resourceUrl.getKey(), resourceConfig);
			}

		}
		SearchParam2FhirPathRegistry s2f = new SearchParam2FhirPathRegistry();
		/*
		 * for (DefaultKeyValue<String, String> searchParamFhirPath :
		 * searchParamsFhirPath) {
		 * s2f.put(searchParamFhirPath.getKey(), searchParamFhirPath.getValue());
		 * }
		 */
		String url = setupUrl + "/SearchParameter";
		do {
			Bundle searchParameters = cr.getClient(setupUrl).search().byUrl(url).returnBundle(Bundle.class).execute();

			searchParameters.getEntry().forEach(x -> {
				SearchParameter sp = (SearchParameter) x.getResource();
				sp.getBase().forEach(base -> {
					s2f.put(base + "." + sp.getCode(), sp.getExpression());
				});
			});
			url = searchParameters.getLink(IBaseBundle.LINK_NEXT) == null ? null
					: searchParameters.getLink(IBaseBundle.LINK_NEXT).getUrl();
		} while (url != null);

		registerProvider(new FederatedSearchProvider(cr, rr, this.getFhirContext(), s2f));
		setPagingProvider(new MapDbPagingProvider(this.getFhirContext(),
				new File(System.getProperty("java.io.tmpdir") + File.separator + "paging.db"), 10, 100));
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
