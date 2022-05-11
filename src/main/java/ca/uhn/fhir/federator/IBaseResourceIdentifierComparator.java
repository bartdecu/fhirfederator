package ca.uhn.fhir.federator;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Identifier;

public class IBaseResourceIdentifierComparator implements Comparator<IBaseResource> {

    @Override
    public int compare(IBaseResource o1, IBaseResource o2) {
        if (o1==null || o2==null){
            return -1;
        }

        List<Identifier> o1Identifiers = new GetIdentifierHelper(o1.getClass()).getIdentifier(o1);
        List<Identifier> o2Identifiers = new GetIdentifierHelper(o2.getClass()).getIdentifier(o2);

        for (Identifier o1Identifier: o1Identifiers){
            for (Identifier o2Identifier: o2Identifiers){
                if (Objects.equals(o1Identifier.getSystem(), o2Identifier.getSystem()) && Objects.equals(o1Identifier.getValue(), o2Identifier.getValue()) ){
                    return 0;
                }
            }
        }
        
        return 1;
    }
    
}
