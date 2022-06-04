package ca.uhn.fhir.federator.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;

public class IncludeNode implements Node {
    private List<ParameterNode> parameterNodes;
    private Node referenceNode;

    public IncludeNode(Node referenceNode, List<ParameterNode> parameterNodes){
        this.referenceNode = referenceNode;
        this.parameterNodes = parameterNodes;
    }

    @Override
    public IBundleProvider execute() {
        IBundleProvider refBundle = referenceNode.execute();
        List<IBundleProvider> includes = parameterNodes.stream().map(x -> x.executeWithReference(refBundle)).collect(Collectors.toList());
        List<IBundleProvider> providers = new ArrayList<>();
        providers.add(refBundle);
        providers.addAll(includes);
        return new SimpleBundleProvider(providers.stream().flatMap(x -> x.getAllResources().stream()).collect(Collectors.toList()));    
    }
    
}
