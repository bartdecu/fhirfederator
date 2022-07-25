package ca.uhn.fhir.federator.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;

public class IncludeNode implements Node {
  private final ParameterNode parameterNode;
  private final Node referenceNode;

  public IncludeNode(Node referenceNode, ParameterNode parameterNode) {
    this.referenceNode = referenceNode;
    this.parameterNode = parameterNode;
  }

  @Override
  public IBundleProvider execute() {
    IBundleProvider refBundle = referenceNode.execute();
    if (refBundle.getAllResources().isEmpty()) {
      return new SimpleBundleProvider();
    }
    List<IBundleProvider> providers = new ArrayList<>();
    providers.add(refBundle);
    IBundleProvider include = parameterNode.executeWithReference(refBundle);
    providers.add(include);
    while (parameterNode.isIterate() && !include.isEmpty()) {
      include = parameterNode.executeWithReference(include);
      providers.add(include);
    }
    return new SimpleBundleProvider(
        providers.stream().flatMap(x -> x.getAllResources().stream()).collect(Collectors.toList()));
  }

  @Override
  public String toString() {
    return "IncludeNode [parameterNode=" + parameterNode + ", referenceNode=" + referenceNode + "]";
  }
}
