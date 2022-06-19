package ca.uhn.fhir.federator;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.antlr.v4.runtime.ParserRuleContext;

import ca.uhn.fhir.federator.FhirUrlParser.FContext;
import ca.uhn.fhir.federator.FhirUrlParser.JContext;
import ca.uhn.fhir.federator.FhirUrlParser.KContext;
import ca.uhn.fhir.federator.FhirUrlParser.MContext;
import ca.uhn.fhir.federator.FhirUrlParser.PContext;
import ca.uhn.fhir.federator.FhirUrlParser.RContext;
import ca.uhn.fhir.federator.FhirUrlParser.SContext;
import ca.uhn.fhir.federator.FhirUrlParser.VContext;

public class ParsedUrlCreator {
  private FContext resourceCtx;
  private PContext httpParam;
  

  public ParsedUrlCreator(ParserRuleContext resource, ParserRuleContext httpParam) {
    this.resourceCtx = (FContext) resource;
    this.httpParam = (PContext) httpParam;
  }

  public Optional<ParsedUrl> createUrl() {
    ParserRuleContext parent = resourceCtx.getParent();
    String resource = ((FContext) resourceCtx).IDENTIFIER().getText();
    ParsedUrl url = null;

    String clazz = parent.getClass().getSimpleName();

    switch (clazz) {
      case "VContext":
        if (httpParam.k().q() != null && "_include".equals(httpParam.k().q().SPECIAL().getText())) {
          String target = ((VContext) parent).e().getText();
          String altResource = null;// check at runtime, but maybe there is a hint
          if (((VContext) parent).b() != null) {
            altResource = ((VContext) parent).b().getText();
          }
          List<String> key= Arrays.asList("identifier");
          String source = resource;
          boolean iterate = false;
          if (httpParam.k().u() != null && "iterate".equals(httpParam.k().u().IDENTIFIER().getText())) {
            iterate = true;
          }

          url = new ParsedUrl(iterate, altResource, key, source, Arrays.asList(target, "identifier"));
        } else if (((VContext) parent).e() != null) {
          ParserRuleContext ref = parent;
          while (!(ref instanceof SContext)) {
            ref = ref.getParent();
          }
          url = new ParsedUrl(resource, Arrays.asList(((VContext) parent).e().IDENTIFIER().getText() , "identifier"),
              ((SContext) ref).a().f().IDENTIFIER().getText(), Arrays.asList("identifier"));
        } else {
          url = new ParsedUrl(resource, ((VContext) parent).i().IDENTIFIER().getText());

        }
        break;
      case "JContext":
        FhirUrlAnalyser a = new FhirUrlAnalyser();
        parent.accept(a);
        if (a.getResources().get(0).size() > 1) {
          ParserRuleContext parent2 = ((FContext) a.getResources().get(0).get(1)).getParent();
          String clazz2 = parent2.getClass().getSimpleName();
          List<String> target = null;
          switch (clazz2) {
            case "JContext":
              target = Arrays.asList(((JContext) parent2).l().IDENTIFIER().getText(), "identifier");
              break;
            case "RContext":
              target = Arrays.asList("identifier");
              break;
          }
          List<String> source = null;
          if (((JContext) parent).m().e() != null) {
            source = Arrays.asList(((JContext) parent).m().e().IDENTIFIER().getText() , "identifier");
          } else {
            source = Arrays.asList("identifier");
          }
          url = new ParsedUrl(resource, source, ((FContext) a.getResources().get(0).get(1)).IDENTIFIER().getText(),
              target);

        } else {
          ParserRuleContext temp = resourceCtx;
          while (!(temp instanceof PContext)) {
            temp = temp.getParent();
          }
          PContext httpParam = (PContext)temp;
          List<String> key= ((JContext) resourceCtx.getParent()).m()==null?null:Arrays.asList(((JContext) resourceCtx.getParent()).m().getText());
          String value = httpParam.d()==null?null:httpParam.d().getText();
          url = new ParsedUrl(resource, key, value);
        }
        break;

      case "RContext":
        FhirUrlAnalyser b = new FhirUrlAnalyser();
        parent.accept(b);
        if (b.getResources().get(0).size() > 1) {
          url = new ParsedUrl(resource, Arrays.asList("identifier"),
              ((FContext) b.getResources().get(0).get(1)).IDENTIFIER().getText(), Arrays.asList("identifier"));

        } else {
          ParserRuleContext temp = resourceCtx;
          while (!(temp instanceof PContext)) {
            temp = temp.getParent();
          }
          PContext httpParam = (PContext) temp;
          List<String> key= ((RContext) resourceCtx.getParent()).t() == null ? null
              : Arrays.asList( ((RContext) resourceCtx.getParent()).t().getText());
          String value = httpParam.d() == null ? null : httpParam.d().getText();
          url = new ParsedUrl(resource, key, value);
        }
        break;
      case "AContext":
        if (httpParam != null) {
          FhirUrlAnalyser c = new FhirUrlAnalyser();
          httpParam.accept(c);
          if (c.getResources().size() != 0 && c.getResources().get(0).size() > 0) {
            ParserRuleContext parent2 = ((FContext) c.getResources().get(0).get(0)).getParent();
            String clazz2 = parent2.getClass().getSimpleName();
            List<String> target = null;
            List<String> source = null;
            switch (clazz2) {
              case "VContext":
                if (((PContext) httpParam).k().e() != null) {
                  source = Arrays.asList(((PContext) httpParam).k().e().IDENTIFIER().getText(), "identifier");
                  target = Arrays.asList("identifier");
                } else {
                  String specialParameter = ((PContext) httpParam).k().q().SPECIAL().getText();
                  if (specialParameter.equals("_revinclude")) {
                    source = null;
                    target = null;
                  } else
                  // TODO
                  if (specialParameter.equals("_include")) {
                    source = null;
                    target = null;
                  } else {
                    source = Arrays.asList(specialParameter);
                  }
                }

                break;
              case "JContext":
                target = Arrays.asList(((JContext) parent2).l().IDENTIFIER().getText(), "identifier");
                source = Arrays.asList("identifier");
                break;
              case "RContext":
                target = Arrays.asList("identifier");// ((RContext)parent2).t().IDENTIFIER().getText();
                if (((RContext) parent2).getParent() instanceof KContext) {
                  source = Arrays.asList(((KContext) ((RContext) parent2).getParent()).e().IDENTIFIER().getText(),"identifier");
                } else {
                  source = Arrays.asList(((MContext) ((RContext) parent2).getParent()).e().IDENTIFIER().getText(), "identifier");
                }
                break;
            }
            url = new ParsedUrl(resource, source, ((FContext) c.getResources().get(0).get(0)).IDENTIFIER().getText(),
                target);
          } else {
            List<String> source = Arrays.asList(httpParam.k().getText());
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
