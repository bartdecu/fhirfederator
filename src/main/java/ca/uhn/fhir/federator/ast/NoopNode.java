package ca.uhn.fhir.federator.ast;

import java.util.Collections;
import java.util.List;

import org.hl7.fhir.instance.model.api.IBaseResource;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;

public class NoopNode implements Node {
  private static final List<IBaseResource> EMPTY_LIST = Collections.emptyList();

  private static class EmptyNoopNode extends NoopNode {

    private static final IBundleProvider EMPTY = new SimpleBundleProvider(EMPTY_LIST);

    @Override
    public IBundleProvider execute() {

      return EMPTY;
    }
  }

  /** */
  public static final NoopNode EMPTY = new EmptyNoopNode();

  private List<IBaseResource> resources;

  public NoopNode() {
    resources = EMPTY_LIST;
  }

  public NoopNode(List<IBaseResource> resources) {
    this.resources = resources;
  }

  @Override
  public IBundleProvider execute() {
    return new SimpleBundleProvider(resources);
  }
}
