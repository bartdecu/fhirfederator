package ca.uhn.fhir.federator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.ParserRuleContext;

import ca.uhn.fhir.federator.FhirUrlParser.FContext;
import ca.uhn.fhir.federator.FhirUrlParser.JContext;
import ca.uhn.fhir.federator.FhirUrlParser.KContext;
import ca.uhn.fhir.federator.FhirUrlParser.MContext;
import ca.uhn.fhir.federator.FhirUrlParser.PContext;
import ca.uhn.fhir.federator.FhirUrlParser.EContext;
import ca.uhn.fhir.federator.FhirUrlParser.SContext;
import ca.uhn.fhir.federator.FhirUrlParser.VContext;

public class ParsedUrlCreator {
  private final FContext resourceCtx;
  private final PContext httpParam;

  public ParsedUrlCreator(ParserRuleContext resource, ParserRuleContext httpParam) {
    this.resourceCtx = (FContext) resource;
    this.httpParam = (PContext) httpParam;
  }

  public Optional<ParsedUrl> createUrl() {
    ParserRuleContext parent = resourceCtx.getParent();
    String resource = resourceCtx.TOKEN() == null ? null : resourceCtx.TOKEN().getText();
    ParsedUrl url = null;

    String clazz = parent.getClass().getSimpleName();

    switch (clazz) {
      case "VContext":
        if (httpParam.k().q() != null && "_include".equals(httpParam.k().q().SPECIAL().getText())) {
          String target = ((VContext) parent).e().getText();
          String altResource = null; // check at runtime, but maybe there is a hint
          if (((VContext) parent).b() != null) {
            altResource = ((VContext) parent).b().getText();
          }
          List<String> key = List.of("identifier");
          boolean iterate =
              httpParam.k().u() != null && "iterate".equals(httpParam.k().u().TOKEN().getText());

          url =
              new ParsedUrl(
                  iterate, altResource, key, resource, Arrays.asList(target, "identifier"));
        } else if (((VContext) parent).e() != null) {
          ParserRuleContext ref = parent;
          while (!(ref instanceof SContext)) {
            ref = ref.getParent();
          }
          url =
              new ParsedUrl(
                  resource,
                  Arrays.asList(((VContext) parent).e().x().TOKEN().getText(), "identifier"),
                  ((SContext) ref).a().f().TOKEN().getText(),
                  List.of("identifier"));
        } else {
          url = new ParsedUrl(resource, ((VContext) parent).i().getText());
        }
        break;
      case "JContext":
        FhirUrlAnalyser a = new FhirUrlAnalyser();
        parent.accept(a);
        if (a.getResources().get(0).size() > 1) {
          ParserRuleContext parent2 = a.getResources().get(0).get(1).getParent();
          String clazz2 = parent2.getClass().getSimpleName();
          List<String> target = null;
          switch (clazz2) {
            case "JContext":
              target = Arrays.asList(((JContext) parent2).l().TOKEN().getText(), "identifier");
              break;
            case "EContext":
              target = List.of("identifier");
              break;
          }
          List<String> source;
          if (((JContext) parent).m().e() != null) {
            source = Arrays.asList(((JContext) parent).m().e().x().TOKEN().getText(), "identifier");
          } else {
            source = List.of("identifier");
          }
          url =
              new ParsedUrl(
                  resource,
                  source,
                  ((FContext) a.getResources().get(0).get(1)).TOKEN().getText(),
                  target);

        } else {
          ParserRuleContext temp = resourceCtx;
          while (!(temp instanceof PContext)) {
            temp = temp.getParent();
          }
          PContext httpParam = (PContext) temp;
          List<String> key =
              ((JContext) resourceCtx.getParent()).m() == null
                  ? null
                  : Collections.singletonList(((JContext) resourceCtx.getParent()).m().getText());
          String value = httpParam.d() == null ? null : httpParam.d().getText();
          url = new ParsedUrl(resource, key, value);
        }
        break;

      case "EContext":
        FhirUrlAnalyser b = new FhirUrlAnalyser();
        parent.accept(b);
        if (b.getResources().get(0).size() > 1) {
          url =
              new ParsedUrl(
                  resource,
                  List.of("identifier"),
                  ((FContext) b.getResources().get(0).get(1)).TOKEN().getText(),
                  List.of("identifier"));

        } else {
          ParserRuleContext temp = resourceCtx;
          while (!(temp instanceof PContext)) {
            temp = temp.getParent();
          }
          PContext httpParam = (PContext) temp;
          List<String> key =
              ((EContext) resourceCtx.getParent()).e() == null
                  ? null
                  : Collections.singletonList(((EContext) resourceCtx.getParent()).e().getText());
          String value = httpParam.d() == null ? null : httpParam.d().getText();
          url = new ParsedUrl(resource, key, value);
        }
        break;
      case "AContext":
        if (httpParam != null) {
          FhirUrlAnalyser c = new FhirUrlAnalyser();
          httpParam.accept(c);
          if (c.getResources().size() != 0 && c.getResources().get(0).size() > 0) {
            ParserRuleContext parent2 = c.getResources().get(0).get(0).getParent();
            String clazz2 = parent2.getClass().getSimpleName();
            List<String> target = null;
            List<String> source = null;
            switch (clazz2) {
              case "VContext":
                if (httpParam.k().e() != null) {
                  source = Arrays.asList(httpParam.k().e().x().TOKEN().getText(), "identifier");
                  target = List.of("identifier");
                } else {
                  String specialParameter = httpParam.k().q().SPECIAL().getText();
                  if (specialParameter.equals("_revinclude")) {
                  } else
                  // TODO
                  if (specialParameter.equals("_include")) {
                  } else {
                    source = List.of(specialParameter);
                  }
                }

                break;
              case "JContext":
                target = Arrays.asList(((JContext) parent2).l().TOKEN().getText(), "identifier");
                source = List.of("identifier");
                break;
              case "EContext":
                target = List.of("identifier"); // ((RContext)parent2).t().IDENTIFIER().getText();
                if (parent2.getParent() instanceof KContext) {
                  source =
                      Arrays.asList(
                          ((KContext) parent2.getParent()).e().x().TOKEN().getText(), "identifier");
                } else {
                  source =
                      Arrays.asList(
                          ((MContext) parent2.getParent()).e().x().TOKEN().getText(), "identifier");
                }
                break;
            }
            url =
                new ParsedUrl(
                    resource,
                    source,
                    ((FContext) c.getResources().get(0).get(0)).TOKEN() == null
                        ? null
                        : ((FContext) c.getResources().get(0).get(0)).TOKEN().getText(),
                    target);
          } else {
            List<String> source = Collections.singletonList(httpParam.k().getText());
            String value = httpParam.d() == null ? null : httpParam.d().getText();
            url = new ParsedUrl(resource, source, value);
          }
        } else {
          url = new ParsedUrl(parent.getText());
        }

        break;

      default:
    }

    return Optional.ofNullable(url);
  }
}
