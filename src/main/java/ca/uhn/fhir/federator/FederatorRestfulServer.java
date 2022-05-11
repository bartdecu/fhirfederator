package ca.uhn.fhir.federator;

import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;

@WebServlet("/*")
public class FederatorRestfulServer extends RestfulServer {

	@Override
	protected void initialize() throws ServletException {
		// Create a context for the appropriate version
		setFhirContext(FhirContext.forR4());
		ClientRegistry cr = new ClientRegistry(Arrays.<String>asList("https://server.fire.ly/R4","https://hapi.fhir.org/baseR4"));
		ResourceRegistry rr = new ResourceRegistry("https://server.fire.ly/R4");
		rr.putServer4Resource("Observation", "https://hapi.fhir.org/baseR4");
		rr.putServer4Resource("Patient", "https://hapi.fhir.org/baseR4");
		rr.putServer4Resource("Patient", "https://server.fire.ly/R4");
		rr.putServer4Resource("Practitioner", "https://server.fire.ly/R4");
		rr.putServer4Resource("AuditEvent","https://hapi.fhir.org/baseR4");
		SearchParam2FhirPathRegistry s2f = new SearchParam2FhirPathRegistry();
    	s2f.put("AuditEvent.entity", "AuditEvent.entity.what");
		s2f.put("identifier","identifier");

		registerProvider(new FederatedSearchProvider(cr,rr, this.getFhirContext(),s2f));
		registerInterceptor(new FederatorInterceptor());
		registerInterceptor(new ResponseHighlighterInterceptor());
	}
}
