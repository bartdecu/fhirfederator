package ca.uhn.fhir.federator;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBase;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.fhirpath.IFhirPath;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class ParameterExecutor {
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ParameterExecutor.class);
    private List<ParsedUrl> urlsPerParameter;
    private ResourceRegistry rr;
    private ClientRegistry cr;
    private FhirContext ctx;
    private SearchParam2FhirPathRegistry s2f;
    private Map<String, List<IBaseResource>> resourceCachePerParameter;

    public ParameterExecutor(List<ParsedUrl> urlsPerParameter, ResourceRegistry rr, ClientRegistry cr, FhirContext ctx, SearchParam2FhirPathRegistry s2f) {
        this.urlsPerParameter = urlsPerParameter;
        this.rr = rr;
        this.cr = cr;
        this.ctx =ctx;
        this.s2f = s2f;
        resourceCachePerParameter = new HashMap<>();
    }

    public List<IBaseResource> execute() {
        for (int i = (urlsPerParameter.size() - 1); i >= 0; i--) {

            ParsedUrl url = urlsPerParameter.get(i);
            String resource = url.getResource();
            List<ParsedUrl> executableUrls = createExecutableUrl(resourceCachePerParameter, url, rr, this.ctx, this.s2f);
            for (ParsedUrl executableUrl : executableUrls) {
                List<IBaseResource> result = null;
                if (!executableUrl.isExecutable()) {
                    result = Collections.emptyList();
                } else {
                    List<ResourceConfig> servers = rr.getServer4Resource(resource);
                    Stream<ResourceConfig> stream = servers.parallelStream();
                    result = stream.flatMap(resourceConfig -> {

                        IGenericClient client = cr.getClient(resourceConfig.getServer());
                        String completeUrl;
                        completeUrl = resourceConfig.getServer() + "/" + executableUrl.toString();
                        ourLog.info("Client request Url: {}", completeUrl);
                        return getResultsForURL(client, completeUrl).stream();
                    }).collect(Collectors.<IBaseResource>toList());
                }
                List<IBaseResource> list = resourceCachePerParameter.get(resource);
                if (list == null || list.isEmpty()) {
                    resourceCachePerParameter.put(resource, result);
                } else {
                    list.addAll(result);
                }
            }

        }

        List<IBaseResource> parameterResources = resourceCachePerParameter.get(urlsPerParameter.get(0).getResource());
        return parameterResources;

    }

    public static List<ParsedUrl> createExecutableUrl(Map<String, List<IBaseResource>> idMap, ParsedUrl url,
            ResourceRegistry rr, FhirContext ctx, SearchParam2FhirPathRegistry s2f) {
        List<ParsedUrl> retVal = new ArrayList<>();
        String resource = url.getResource();
        if (url.getPlaceholder() != null) {
            if (idMap.get(url.getPlaceholder().getKey()) != null) {
                List<IBaseResource> list = idMap.get(url.getPlaceholder().getKey());
                List<String> identifiers = getIdentifiersFromResources(list, url.getPlaceholder(), ctx,
                        s2f);
                int batch = rr.getMaxOr4Resource(resource);
                int counter = 0;
                List<String> idBatch = new ArrayList<>();
                for (String identifier : identifiers) {
                    if (counter < batch) {
                        idBatch.add(identifier);
                    } else {
                        counter = 0;
                        retVal.add(new ParsedUrl(resource, url.getKey(), StringUtils.join(idBatch, ",")));
                        idBatch.clear();
                        idBatch.add(identifier);
                    }
                    counter++;
                }
                if (!idBatch.isEmpty()) {
                    retVal.add(new ParsedUrl(resource, url.getKey(), StringUtils.join(idBatch, ",")));
                }
            }
        } else {
            retVal.add(url);
        }
        return retVal;
    }

    private static List<String> getIdentifiersFromResources(List<IBaseResource> list,
            DefaultMapEntry<String, List<String>> placeholder, FhirContext ctx, SearchParam2FhirPathRegistry s2f) {
        final List<IBase> toProcess = new ArrayList<>(list);
        final List<IBase> nextRound = new ArrayList<>();

        placeholder.getValue().forEach(searchParam -> {

            toProcess.forEach(inputResource -> {
                String resourceName = inputResource.getClass().getSimpleName();
                List<IBase> outputs;
                IFhirPath fhirPath = ctx.newFhirPath();
                String fp = s2f.getFhirPath(resourceName, searchParam);
                outputs = fhirPath.evaluate(inputResource, fp, IBase.class);
                nextRound.addAll(outputs);

            });

            toProcess.clear();
            toProcess.addAll(nextRound);
            nextRound.clear();

        });

        List<String> identifiers = toProcess.stream()
                .flatMap(x -> {
                    Identifier id = (Identifier) x;
                    Stream<String> out = null;
                    if (id.getValue() == null) {
                        out = Collections.<String>emptyList().stream();
                    } else if (id.getSystem() == null) {
                        out = Arrays.asList(id.getValue()).stream();
                    } else {
                        out = Arrays.<String>asList(id.getSystem() + "|" + id.getValue()).stream();
                    }
                    return out;
                }).distinct()

                .map(x -> {
                    String encoded = null;
                    try {
                        encoded = URLEncoder.encode(x, "UTF-8");
                    } catch (Exception e) {
                    }
                    ;
                    return encoded;
                })

                .collect(Collectors.<String>toList());
        return identifiers;
    }


    private static List<IBaseResource> getResultsForURL(IGenericClient client, String completeUrl) {
        Bundle bundle = client.search().byUrl(completeUrl).returnBundle(Bundle.class).execute();
        List<BundleLinkComponent> next = bundle.getLink().stream().filter(link -> link.getRelation().equals("next"))
            .collect(Collectors.toList());
    
        List<IBaseResource> own = bundle.getEntry().stream().filter(bec -> {
          return bec.getResource() instanceof IBaseResource;
        })
            .map(bec -> ((IBaseResource) bec.getResource())).collect(Collectors.toList());
        ourLog.info("Client request Url: {} #{}", completeUrl, own.size());
        List<IBaseResource> out = new ArrayList<>();
        out.addAll(own);
        if (!next.isEmpty()) {
          List<IBaseResource> other = getResultsForURL(client, next.get(0).getUrl());
          out.addAll(other);
        }
        return out;
      }

    public void setCachedResources(Map<String, List<IBaseResource>> resourceCachePerParameter) {
        this.resourceCachePerParameter = resourceCachePerParameter;
    }

}
