package ca.uhn.fhir.federator.ast;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.federator.IBaseResourceIdentifierComparator;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;

public class AndNode implements Node {
    private List<Node> nodes;

    public AndNode(List<Node> nodes) {
        this.nodes = nodes;

    }

    @Override
    public IBundleProvider execute() {
        List<IBundleProvider> results = nodes.stream().map(x -> x.execute()).collect(Collectors.toList());
        List<IBaseResource> and = results.stream().map(x -> x.getAllResources()).reduce((a,b)->{

            return intersection(a, b);

        }).orElse(Collections.emptyList());
        return new SimpleBundleProvider(and);
    }

    private List<IBaseResource> intersection(List<IBaseResource> list, List<IBaseResource> list2) {
        return list.stream()
                .distinct()
                .filter(x -> list2.stream().map(y -> new IBaseResourceIdentifierComparator().compare(x, y) == 0)
                        .reduce((a, b) -> a || b).orElse(false))
                .collect(Collectors.toList());
    }

}
