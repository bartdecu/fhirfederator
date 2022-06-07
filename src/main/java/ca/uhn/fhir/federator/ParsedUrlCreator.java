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
          String altResource = null;//check at runtime, but maybe there is a hint
          if ( ((VContext) parent).b()!=null){
            altResource =  ((VContext) parent).b().getText();
          }
          String key = "identifier";
          String source = resource;
          boolean iterate = false;
          if (httpParam.k().u()!=null && "iterate".equals(httpParam.k().u().IDENTIFIER().getText())){
            iterate = true;
          }
          
          url = new ParsedUrl(iterate,altResource, key, source, Arrays.asList(target, "identifier"));
        } else if (((VContext) parent).e() != null) {
          ParserRuleContext ref = parent;
          while (!(ref instanceof SContext)) {
            ref = ref.getParent();
          }
          url = new ParsedUrl(resource, ((VContext) parent).e().IDENTIFIER().getText()+".identifier",
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
          String source = null;
          if (((JContext) parent).m().e() != null) {
            source = ((JContext) parent).m().e().IDENTIFIER().getText() + ".identifier";
          } else {
            source = "identifier";
          }
          url = new ParsedUrl(resource, source, ((FContext) a.getResources().get(0).get(1)).IDENTIFIER().getText(),
              target);

        } else {

          ParserRuleContext httpParam = resourceCtx;
          while (!(httpParam instanceof PContext)) {
            httpParam = httpParam.getParent();
          }

          url = new ParsedUrl(resource, ((JContext) resourceCtx.getParent()).m().getText(),
              ((PContext) httpParam).d().getText());

        }
        break;

      case "RContext":
        FhirUrlAnalyser b = new FhirUrlAnalyser();
        parent.accept(b);
        if (b.getResources().get(0).size() > 1) {
          url = new ParsedUrl(resource, "identifier",
              ((FContext) b.getResources().get(0).get(1)).IDENTIFIER().getText(), Arrays.asList("identifier"));

        } else {
          ParserRuleContext httpParam = resourceCtx;
          while (!(httpParam instanceof PContext)) {
            httpParam = httpParam.getParent();
          }
          url = new ParsedUrl(resource, ((RContext) resourceCtx.getParent()).t().getText(),
              ((PContext) httpParam).d().getText());
        }
        break;
      case "AContext":
        if (httpParam != null){
        FhirUrlAnalyser c = new FhirUrlAnalyser();
        httpParam.accept(c);
        if (c.getResources().size() != 0 && c.getResources().get(0).size() > 0) {
          ParserRuleContext parent2 = ((FContext) c.getResources().get(0).get(0)).getParent();
          String clazz2 = parent2.getClass().getSimpleName();
          List<String> target = null;
          String source = null;
          switch (clazz2) {
            case "VContext":
              if (((PContext) httpParam).k().e() != null) {
                source = ((PContext) httpParam).k().e().IDENTIFIER().getText() + ".identifier";
                target = Arrays.asList("identifier");
              } else {
                source = ((PContext) httpParam).k().q().SPECIAL().getText();
                if (source.equals("_revinclude")) {
                  source = null;
                  target = null;
                } else
                // TODO
                if (source.equals("_include")) {
                  source = null;
                  target = null;
                }
              }

              break;
            case "JContext":
              target = Arrays.asList(((JContext) parent2).l().IDENTIFIER().getText(), "identifier");
              source = "identifier";
              break;
            case "RContext":
              target = Arrays.asList("identifier");// ((RContext)parent2).t().IDENTIFIER().getText();
              if (((RContext) parent2).getParent() instanceof KContext) {
                source = ((KContext) ((RContext) parent2).getParent()).e().IDENTIFIER().getText() + ".identifier";
              } else {
                source = ((MContext) ((RContext) parent2).getParent()).e().IDENTIFIER().getText() + ".identifier";
              }
              break;
          }
          url = new ParsedUrl(resource, source, ((FContext) c.getResources().get(0).get(0)).IDENTIFIER().getText(),
              target);
        } else {
          String source = ((PContext) httpParam).k().getText();
          url = new ParsedUrl(resource, source, ((PContext) httpParam).d().getText());
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
