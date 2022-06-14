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
import org.hl7.fhir.r4.model.Bundle.BundleLinkComponent;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;
import ca.uhn.fhir.rest.client.api.IGenericClient;

public class ParameterExecutor {
    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ParameterExecutor.class);
    private List<ParsedUrl> urlsPerParameter;
    private ResourceRegistry rr;
    private ClientRegistry cr;
    private FhirContext ctx;
    private SearchParam2FhirPathRegistry s2f;
    private Map<String, List<IBaseResource>> resourceCachePerParameter;

    public ParameterExecutor(List<ParsedUrl> urlsPerParameter, ResourceRegistry rr, ClientRegistry cr, FhirContext ctx,
            SearchParam2FhirPathRegistry s2f) {
        this.urlsPerParameter = urlsPerParameter;
        this.rr = rr;
        this.cr = cr;
        this.ctx = ctx;
        this.s2f = s2f;
        resourceCachePerParameter = new HashMap<>();
    }

    public List<IBaseResource> execute() {
        for (int i = (urlsPerParameter.size() - 1); i >= 0; i--) {

            ParsedUrl url = urlsPerParameter.get(i);
            String resource = url.getResource();
            resourceCachePerParameter.put(resource, new ArrayList<>());
            List<ParsedUrl> executableUrls = createExecutableUrl(url);
            for (ParsedUrl executableUrl : executableUrls) {
                List<ResourceConfig> servers = rr.getServer4Resource(resource);
                Stream<ResourceConfig> stream = servers.parallelStream();
                List<IBaseResource> result = stream.flatMap(resourceConfig -> executeUrl(executableUrl, resourceConfig))
                        .collect(Collectors.<IBaseResource>toList());
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

    private Stream<? extends IBaseResource> executeUrl(ParsedUrl executableUrl, ResourceConfig resourceConfig) {
        if (!executableUrl.isExecutable()) {
            return Stream.empty();
        } else {
            IGenericClient client = cr.getClient(resourceConfig.getServer());
            String completeUrl;
            completeUrl = resourceConfig.getServer() + "/" + executableUrl.toString();
            ourLog.info("Client request Url: {}", completeUrl);
            return getResultsForURL(client, completeUrl).stream();
        }
    }

    public List<ParsedUrl> createExecutableUrl(ParsedUrl url) {
        List<ParsedUrl> retVal = new ArrayList<>();
        List<String> resources = Arrays.asList(url.getResource());
        if (url.getResource() == null) {
            // _include case
            List<IBaseResource> list = resourceCachePerParameter.getOrDefault(url.getPlaceholder().getKey(),
                    Collections.emptyList());
            List<String> chainedSearchParams = url.getPlaceholder().getValue();
            chainedSearchParams = trimIdentifierFromChainedSearchParameters(chainedSearchParams);
            List<IBase> toResources = list.stream().map(x -> (IBase) x).collect(Collectors.toList());
            List<IBase> classes = new ResourceCollector(ctx, cr, rr, s2f, toResources, chainedSearchParams).execute();
            resources = classes.stream().map(resource -> createResourceFromReference(resource))
                    .map(y -> y.getClass().getSimpleName())
                    .distinct()
                    .collect(Collectors.toList());

        }
        for (String resource : resources) {
            if (url.getPlaceholder() != null) {
                if (resourceCachePerParameter.get(url.getPlaceholder().getKey()) != null) {
                    List<IBaseResource> list = resourceCachePerParameter.getOrDefault(url.getPlaceholder().getKey(),
                            Collections.emptyList());
                    List<String> identifiers = getIdentifiersFromResources(list, url.getPlaceholder());
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
        }
        return retVal;
    }

    private IBase createResourceFromReference(IBase resource) {
        IBase baseResource = null;
        if (resource instanceof Reference) {
            baseResource = new ResourceCollector(ctx, cr, rr, s2f, Arrays.asList(resource), Arrays.asList("resolve()"))
                    .execute().get(0);
        } else {
            baseResource = resource;
        }
        return baseResource;
    }

    private List<String> trimIdentifierFromChainedSearchParameters(List<String> chainedSearchParams) {
        if ("identifier".equals(chainedSearchParams.get(chainedSearchParams.size() - 1))) {
            chainedSearchParams = chainedSearchParams.subList(0, chainedSearchParams.size() - 1);
        }
        return chainedSearchParams;
    }

    private List<String> getIdentifiersFromResources(List<IBaseResource> list,
            DefaultMapEntry<String, List<String>> placeholder) {
        List<String> chainedSearchParams = placeholder.getValue();
        chainedSearchParams = trimIdentifierFromChainedSearchParameters(chainedSearchParams);
        List<IBase> toProcess = new ResourceCollector(ctx, cr, rr, s2f,
                list.stream().map(x -> (IBase) x).collect(Collectors.toList()), chainedSearchParams).execute();
        toProcess = toProcess.stream().flatMap(resource -> handleReferenceResource(resource))
                .collect(Collectors.toList());

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

    private Stream<? extends IBase> handleReferenceResource(IBase resource) {
        if (resource instanceof Reference) {
            Reference ref = (Reference) resource;
            if (ref.getIdentifier() != null && !ref.getIdentifier().isEmpty()) {
                return Arrays.asList(ref.getIdentifier()).stream();
            } else {
                return new ResourceCollector(ctx, cr, rr, s2f, Arrays.asList(resource),
                        Arrays.asList("resolve()", "identifier")).execute().stream();
            }
        } else {
            return new ResourceCollector(ctx, cr, rr, s2f, Arrays.asList(resource), Arrays.asList("identifier"))
                    .execute().stream();
        }
    }

    private static List<IBaseResource> getResultsForURL(IGenericClient client, String completeUrl) {
        List<IBaseResource> out = new ArrayList<>();
        Bundle bundle = new Bundle();
        try {
            bundle = client.search().byUrl(completeUrl).returnBundle(Bundle.class).execute();
        } catch (RuntimeException e) {
            ourLog.error("Request failed: {} {}", completeUrl, e.getMessage());
        }
        List<BundleLinkComponent> next = bundle.getLink().stream().filter(link -> link.getRelation().equals("next"))
                .collect(Collectors.toList());

        List<IBaseResource> own = bundle.getEntry().stream().filter(bec -> {
            return bec.getResource() instanceof IBaseResource;
        })
                .map(bec -> ((IBaseResource) bec.getResource())).collect(Collectors.toList());
        ourLog.info("Client request Url: {} #{}", completeUrl, own.size());

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
