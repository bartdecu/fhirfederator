package ca.uhn.fhir.federator.ast;

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
        Node andNode = nodes.stream().reduce((a,b)-> {
            List<IBaseResource> aList = a.execute().getAllResources();
            if (aList.isEmpty()){
                return NoopNode.EMPTY;
            }
            List<IBaseResource> bList = b.execute().getAllResources();
            if (bList.isEmpty()){
                return NoopNode.EMPTY;
            }
            return new NoopNode(intersection(aList, bList));

        }).orElse(NoopNode.EMPTY);
        return new SimpleBundleProvider(andNode.execute().getAllResources());
    }

    private List<IBaseResource> intersection(List<IBaseResource> list, List<IBaseResource> list2) {
        return list.stream()
                .distinct()
                .filter(x -> list2.stream().map(y -> new IBaseResourceIdentifierComparator().compare(x, y) == 0)
                        .reduce((a, b) -> a || b).orElse(false))
                .collect(Collectors.toList());
    }

}
