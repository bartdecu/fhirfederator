package ca.uhn.fhir.federator;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.jetbrains.annotations.NotNull;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.IVersionSpecificBundleFactory;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.IPagingProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;

public class MapDbPagingProvider implements IPagingProvider {

  public static class ListIBaseResourceSerializer implements Serializer<List<IBaseResource>> {
    private final FhirContext ctx;

    public ListIBaseResourceSerializer(FhirContext ctx) {
      this.ctx = ctx;
    }

    @Override
    public void serialize(DataOutput2 out, @NotNull List<IBaseResource> value) throws IOException {

      IVersionSpecificBundleFactory fac = ctx.newBundleFactory();
      fac.addResourcesToBundle(value, BundleTypeEnum.COLLECTION, null, null, null);

      // Instantiate a new JSON parser
      IParser parser = ctx.newJsonParser();

      // Serialize it
      String serialized = parser.encodeResourceToString(fac.getResourceBundle());

      out.writeUTF(serialized);
    }

    @Override
    public List<IBaseResource> deserialize(DataInput2 input, int available) throws IOException {
      String theMessageString = input.readUTF();
      IParser parser = ctx.newJsonParser();
      Bundle bundle = (Bundle) parser.parseResource(theMessageString);
      return bundle.getEntry().stream().map(BundleEntryComponent::getResource).collect(Collectors.toList());
    }
  }

  private final int defaultPageSize;
  private final int maximumPageSize;
  private final DB db;
  final HTreeMap<String, List<IBaseResource>> map;
  private static final org.slf4j.Logger ourLog =
      org.slf4j.LoggerFactory.getLogger(MapDbPagingProvider.class);

  public MapDbPagingProvider(FhirContext ctx, File file, int defaultPageSize, int maximumPageSize) {
    this.defaultPageSize = defaultPageSize;
    this.maximumPageSize = maximumPageSize;
    db = DBMaker.fileDB(file.getAbsolutePath()).fileMmapEnable().closeOnJvmShutdown().make();
    map =
        db.hashMap("paging", Serializer.STRING, new ListIBaseResourceSerializer(ctx))
            .createOrOpen();
  }

  @Override
  public int getDefaultPageSize() {
    return this.defaultPageSize;
  }

  @Override
  public int getMaximumPageSize() {

    return maximumPageSize;
  }

  @Override
  public IBundleProvider retrieveResultList(RequestDetails theRequestDetails, @NotNull String theSearchId) {
    SimpleBundleProvider retVal = null;

    try {
      retVal = new SimpleBundleProvider(map.getOrDefault(theSearchId, Collections.emptyList()));
    } catch (Exception e) {
      ourLog.error(e.getMessage(), e);
    }

    return retVal;
  }

  @Override
  public String storeResultList(RequestDetails theRequestDetails, IBundleProvider theList) {
    UUID uuid = UUID.randomUUID();
    String id = uuid.toString();

    try {

      map.put(id, theList.getAllResources());

    } catch (Exception e) {
      ourLog.error(e.getMessage(), e);
    }

    return id;
  }

  protected void finalize() {
    db.close();
  }
}
