package ca.uhn.fhir.federator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;

import ca.uhn.fhir.federator.FhirUrlParser.CContext;
import ca.uhn.fhir.federator.FhirUrlParser.FContext;
import ca.uhn.fhir.federator.FhirUrlParser.PContext;

public class FhirUrlAnalyser extends FhirUrlBaseVisitor<Object> {

  private int currentIndex = 0;
  private final List<List<ParserRuleContext>> resources = new ArrayList<>();
  private List<ParserRuleContext> httpParams = new ArrayList<>();
  private final Map<ParserRuleContext, Integer> toIndex = new HashMap<>();

  public List<ParserRuleContext> getAndParameters() {
    List<ParserRuleContext> andParams =
        httpParams.stream()
            .filter(
                p ->
                    ((PContext) p).k().q() == null
                        || !"_revinclude".equals(((PContext) p).k().q().getText())
                            && !"_include".equals(((PContext) p).k().q().getText()))
            .collect(Collectors.toList());
    if (andParams.isEmpty()) {
      andParams = new ArrayList<>();
      andParams.add(null);
    }
    return andParams;
  }

  public List<ParserRuleContext> getIncludeParameters() {
    return httpParams.stream()
        .filter(
            p ->
                ((PContext) p).k().q() != null
                    && ("_include".equals(((PContext) p).k().q().SPECIAL().getText())
                        || "_revinclude".equals(((PContext) p).k().q().SPECIAL().getText())))
        .collect(Collectors.toList());
  }

  public List<List<ParserRuleContext>> getResources() {
    return resources;
  }

  public List<ParserRuleContext> getResourcesForHttpParam(
      boolean dependent, ParserRuleContext httpParam) {
    List<ParserRuleContext> retVal;
    if (httpParam == null) {
      retVal = new ArrayList<>(resources.get(0));
    } else {
      Integer index = toIndex.get(httpParam);
      if (index == null) {
        retVal = Collections.emptyList();
      } else {
        retVal = new ArrayList<>();
        if (!dependent) {
          retVal.addAll(resources.get(0));
        }
        retVal.addAll(resources.get(toIndex.get(httpParam)));
      }
    }
    return retVal;
  }

  public List<ParserRuleContext> getHttpParams() {
    return httpParams;
  }

  @Override
  public Object visitF(FContext ctx) {
    while (resources.size() <= currentIndex) {
      resources.add(new ArrayList<ParserRuleContext>());
    }
    List<ParserRuleContext> temp = resources.get(currentIndex);
    temp.add(ctx);
    return super.visitF(ctx);
  }
  /*
    @Override
    public Object visitE(EContext ctx) {
      while (resources.size() <= currentIndex) {
        resources.add(new ArrayList<ParserRuleContext>());
      }
      List<ParserRuleContext> temp = resources.get(currentIndex);
      if (ctx.x()!=null && ctx.f() == null && ctx.e()!=null){
        temp.add(new FContext(ctx,-1));
      }
      return super.visitE(ctx);
    }
  */
  @Override
  public Object visitP(PContext ctx) {
    httpParams.add(ctx);
    toIndex.put(ctx, currentIndex);
    return super.visitP(ctx);
  }

  @Override
  public Object visitC(CContext ctx) {
    currentIndex++;
    while (resources.size() <= currentIndex) {
      resources.add(new ArrayList<ParserRuleContext>());
    }
    Object result = super.visitC(ctx);
    currentIndex--;
    return result;
  }
}
