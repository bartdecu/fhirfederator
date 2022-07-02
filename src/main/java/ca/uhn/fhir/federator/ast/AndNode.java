package ca.uhn.fhir.federator.ast;

import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.federator.IBaseResourcePredicate;
import ca.uhn.fhir.federator.ResourceRegistry;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;

public class AndNode implements Node {
  private List<Node> nodes;
  private ResourceRegistry rr;

  public AndNode(ResourceRegistry rr, List<Node> nodes) {
    this.nodes = nodes;
    this.rr = rr;
  }

  @Override
  public IBundleProvider execute() {
    Node andNode =
        nodes.stream()
            .reduce(
                (a, b) -> {
                  List<IBaseResource> aList = a.execute().getAllResources();
                  if (aList.isEmpty()) {
                    return NoopNode.EMPTY;
                  }
                  List<IBaseResource> bList = b.execute().getAllResources();
                  if (bList.isEmpty()) {
                    return NoopNode.EMPTY;
                  }
                  return new NoopNode(intersection(rr, aList, bList));
                })
            .orElse(NoopNode.EMPTY);
    return new SimpleBundleProvider(andNode.execute().getAllResources());
  }

  private List<IBaseResource> intersection(
      ResourceRegistry rr, List<IBaseResource> list, List<IBaseResource> list2) {
    return list.stream()
        .distinct()
        .filter(
            x ->
                list2.stream()
                    .map(y -> new IBaseResourcePredicate(rr).test(x, y))
                    .reduce((a, b) -> a || b)
                    .orElse(false))
        .collect(Collectors.toList());
  }
}
