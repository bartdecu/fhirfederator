package ca.uhn.fhir.federator.ast;

import ca.uhn.fhir.rest.api.server.IBundleProvider;

public interface Node {
    IBundleProvider execute();
}
