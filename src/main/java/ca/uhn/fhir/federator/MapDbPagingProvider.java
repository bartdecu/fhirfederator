package ca.uhn.fhir.federator;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.DataInput2;
import org.mapdb.DataOutput2;
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
        private FhirContext ctx;

        public ListIBaseResourceSerializer(FhirContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void serialize(DataOutput2 out, List<IBaseResource> value) throws IOException {

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
            return bundle.getEntry().stream().map(x -> x.getResource()).collect(Collectors.toList());
        }

    }

    private int defaultPageSize;
    private int maximumPageSize;
    private DB db;
    private FhirContext ctx;

    public MapDbPagingProvider(FhirContext ctx, File file, int defaultPageSize, int maximumPageSize) {
        this.defaultPageSize = defaultPageSize;
        this.maximumPageSize = maximumPageSize;
        this.ctx = ctx;
        db = DBMaker
                .fileDB(file.getAbsolutePath())
                .fileMmapEnable()
                .make();
                

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
    public IBundleProvider retrieveResultList(RequestDetails theRequestDetails, String theSearchId) {
        Map<String, List<IBaseResource>> map = db
                .hashMap("paging", Serializer.STRING, new ListIBaseResourceSerializer(ctx))
                .createOrOpen();
        SimpleBundleProvider retVal = new SimpleBundleProvider(map.getOrDefault(theSearchId, Collections.emptyList()));
        //db.close();
        return retVal;
    }

    @Override
    public String storeResultList(RequestDetails theRequestDetails, IBundleProvider theList) {
        Map<String, List<IBaseResource>> map = db
                .hashMap("paging", Serializer.STRING, new ListIBaseResourceSerializer(ctx))
                .createOrOpen();
        UUID uuid = UUID.randomUUID();
        String id = uuid.toString();
        map.put(id, theList.getAllResources());
        //db.close();
        return id;
    }

    protected void finalize() {
        db.close();
    }

}
