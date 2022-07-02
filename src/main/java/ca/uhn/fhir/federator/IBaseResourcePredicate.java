package ca.uhn.fhir.federator;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Identifier;

public class IBaseResourcePredicate implements BiPredicate<IBaseResource, IBaseResource> {
  private ResourceRegistry rr;
  private static final BiPredicate<List<Identifier>, List<Identifier>> FALSE = (t, u) -> false;

  public IBaseResourcePredicate(ResourceRegistry rr) {
    this.rr = rr;
  }

  @Override
  public boolean test(IBaseResource o1, IBaseResource o2) {
    String resource = o1.getClass().getSimpleName();

    if (o1 == null || o2 == null) {
      return false;
    }

    List<List<String>> filter = rr.getServer4Resource(resource).getIdentifiers();

    BiPredicate<List<Identifier>, List<Identifier>> identifierListPredicate = createPredicate(filter);
    List<Identifier> o1Identifiers = new GetIdentifierHelper(o1.getClass()).getIdentifier(o1);
    List<Identifier> o2Identifiers = new GetIdentifierHelper(o2.getClass()).getIdentifier(o2);

    return identifierListPredicate.test(o1Identifiers, o2Identifiers);

  }

  private BiPredicate<List<Identifier>, List<Identifier>> createPredicate(List<List<String>> filter) {

    if (filter == null || filter.isEmpty()) {
      return (t, u) -> {
        for (Identifier o1Identifier : t) {
          for (Identifier o2Identifier : u) {
            return Objects.equals(o1Identifier.getSystem(), o2Identifier.getSystem())
                && Objects.equals(o1Identifier.getValue(), o2Identifier.getValue());
          }
        }
        return false;
      };

    } else {

      return createFilterPredicate(filter);
    }
  }

  private BiPredicate<List<Identifier>, List<Identifier>> createFilterPredicate(List<List<String>> filter) {
    List<List<BiPredicate<List<Identifier>, List<Identifier>>>> bis = filter
        .stream()
        .map(
            ors -> ors
                .stream()
                .map(id -> createBiPredicate(id))
                .collect(Collectors.toList()))
        .collect(Collectors.toList());

    List<BiPredicate<List<Identifier>, List<Identifier>>> bis2 = bis.stream()
        .map(and -> and.stream().reduce((a, b) -> a.or(b)).orElse(FALSE)).collect(Collectors.toList());

    BiPredicate<List<Identifier>, List<Identifier>> result = bis2.stream().reduce((a, b) -> a.and(b)).orElse(FALSE);

    return result;
  }

  private BiPredicate<List<Identifier>, List<Identifier>> createBiPredicate(String system) {
    return (t, u) -> {
      if (t == null || u == null || t.isEmpty() || u.isEmpty()) {
        return false;
      }
      List<Identifier> tMatches = t.stream().filter(x -> system.equals(x.getSystem())).collect(Collectors.toList());
      boolean result = u.stream().anyMatch(i -> tMatches.stream()
          .anyMatch(tMatch -> tMatch.getSystem().equals(i.getSystem()) && tMatch.getValue().equals(i.getValue())));
      return result;
    };

  }
}