package ca.uhn.fhir.federator;

import java.util.List;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.StringUtils;

public class ParsedUrl {
    String resource;
    String key;
    String value;
    DefaultMapEntry<String, List<String>> placeholder;
    public ParsedUrl(String resource, String key, String value) {
        this(resource, key, value, null, null);
    }
    
    public ParsedUrl(String resource, String key, String placeholderResource, List<String> placeholderKey) {
        this(resource, key, null, placeholderResource, placeholderKey);
    }

    private  ParsedUrl(String resource, String key, String value, String placeholderResource, List<String> placeholderKey) {
        this.resource = resource;
        this.key = key;
        this.value = value;
        if (placeholderKey == null || placeholderKey.isEmpty() || placeholderResource == null ){    
            this.placeholder = null;
        } else {
        this.placeholder = new DefaultMapEntry<String,List<String>>(placeholderResource, placeholderKey);
        }
    }
    public ParsedUrl(String resource, String technicalId) {
        this(resource, "_id",technicalId);
    }
    public String getResource() {
        return resource;
    }
    public void setResource(String resource) {
        this.resource = resource;
    }
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }
    public String getValue() {
        return value;
    }
    public void setValue(String value) {
        this.value = value;
    }
    public DefaultMapEntry<String,List<String>> getPlaceholder() {
        return placeholder;
    }
    public void setPlaceholder(DefaultMapEntry<String,List<String>> placeholder) {
        this.placeholder = placeholder;
    }
    public String toString(){
        String retVal = this.resource + '?' + this.key;
        if (this.value != null || this.placeholder != null){
            retVal += "=";
            if (this.value != null){
                retVal += value;
            } else {
                String placeholderValue = StringUtils.join(placeholder.getValue(),".");
                retVal += ("{"+placeholder.getKey() + "." +placeholderValue+"}");
            }
        }
        return retVal;

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((key == null) ? 0 : key.hashCode());
        result = prime * result + ((placeholder == null) ? 0 : placeholder.hashCode());
        result = prime * result + ((resource == null) ? 0 : resource.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ParsedUrl other = (ParsedUrl) obj;
        if (key == null) {
            if (other.key != null)
                return false;
        } else if (!key.equals(other.key))
            return false;
        if (placeholder == null) {
            if (other.placeholder != null)
                return false;
        } else if (!placeholder.equals(other.placeholder))
            return false;
        if (resource == null) {
            if (other.resource != null)
                return false;
        } else if (!resource.equals(other.resource))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }
    
    
}
