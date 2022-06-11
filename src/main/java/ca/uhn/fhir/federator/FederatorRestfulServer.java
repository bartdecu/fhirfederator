package ca.uhn.fhir.federator;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.SearchParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.BaseRuntimeElementDefinition;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

//@Service
public class FederatorRestfulServer extends RestfulServer {

	private final FederatorProperties configuration;
	private static final Logger ourLog = LoggerFactory.getLogger(FederatorRestfulServer.class);

	public FederatorRestfulServer(FederatorProperties configuration) {
		this.configuration = configuration;

		// Create a context for the appropriate version
		setFhirContext(FhirContext.forR4());
		ClientRegistry cr = new ClientRegistry(
				configuration.getMembers().stream().map(x -> x.getUrl()).collect(Collectors.toList()),
				this.getFhirContext());
		ResourceRegistry rr = new ResourceRegistry(configuration.getResources().getDefault());
		for (Entry<String, List<ResourceConfig>> resourceUrl : configuration.resources.other.entrySet()) {
			for (ResourceConfig resourceConfig : resourceUrl.getValue()) {
				rr.putServer4Resource(resourceUrl.getKey(), resourceConfig);
			}

		}
		SearchParam2FhirPathRegistry s2f = new SearchParam2FhirPathRegistry();

		String url = configuration.getSetup().getUrl() + "/SearchParameter";
		do {
			Bundle searchParameters = cr.getClient(configuration.getSetup().getUrl()).search().byUrl(url)
					.returnBundle(Bundle.class).execute();

			searchParameters.getEntry().forEach(x -> {
				SearchParameter sp = (SearchParameter) x.getResource();
				sp.getBase().forEach(base -> {
					s2f.put(base + "." + sp.getCode(), sp.getExpression());
				});
			});
			url = searchParameters.getLink(IBaseBundle.LINK_NEXT) == null ? null
					: searchParameters.getLink(IBaseBundle.LINK_NEXT).getUrl();
		} while (url != null);

		File pagingFile = new File(System.getProperty("java.io.tmpdir") + File.separator + "paging.db");

		if (pagingFile.exists()) {
			pagingFile.delete();
		}

		Map<BaseRuntimeElementDefinition<?>, Class<? extends IBase>> reverse = getReverseMap();

		for (BaseRuntimeElementDefinition rt : getFhirContext().getElementDefinitions()) {

			try {

				Class<? extends IBase> base = rt.getImplementingClass();
				IBase object;
				object = base.getDeclaredConstructor().newInstance();
				if (object instanceof IBaseResource) {
					ourLog.info("Loading {}", rt.getImplementingClass().getSimpleName());
					registerProvider(new FederatedReadProvider(cr, rr, (Class<? extends IBaseResource>) base));
				}
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				ourLog.info(e.getMessage());
			}

		}

		registerProvider(new CapabilityStatementProvider(cr, rr));
		registerProvider(new FederatedSearchProvider(cr, rr, this.getFhirContext(), s2f));
		setPagingProvider(new MapDbPagingProvider(this.getFhirContext(), pagingFile, 10, 100));
		registerInterceptor(new FederatorInterceptor());
		registerInterceptor(new ResponseHighlighterInterceptor());
	}

	private Map<BaseRuntimeElementDefinition<?>, Class<? extends IBase>> getReverseMap() {

		try {
			Field privateField = getFhirContext().getClass().getDeclaredField("myClassToElementDefinition");

			// Set the accessibility as true
			privateField.setAccessible(true);

			// Store the value of private field in variable
			Map<Class<? extends IBase>, BaseRuntimeElementDefinition<?>> name;
			name = (Map<Class<? extends IBase>, BaseRuntimeElementDefinition<?>>) privateField
					.get(getFhirContext());

			Map<BaseRuntimeElementDefinition<?>, Class<? extends IBase>> reverse = new HashMap<>();

			for (Entry<Class<? extends IBase>, BaseRuntimeElementDefinition<?>> entry : name.entrySet()) {
				reverse.put(entry.getValue(), entry.getKey());
			}

			return reverse;

		} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}
}
