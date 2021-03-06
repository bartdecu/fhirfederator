package ca.uhn.fhir.federator;

import java.util.List;

import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.lang3.StringUtils;

public class ParsedUrl {
  String resource;
  List<String> key;
  String value;
  DefaultMapEntry<String, List<String>> placeholder;
  private boolean iterate;

  public boolean isIterate() {
    return iterate;
  }

  public void setIterate(boolean iterate) {
    this.iterate = iterate;
  }

  public ParsedUrl(String resource, List<String> key, String value) {
    this(false, resource, key, value, null, null);
  }

  public ParsedUrl(
      String resource, List<String> key, String placeholderResource, List<String> placeholderKey) {
    this(false, resource, key, null, placeholderResource, placeholderKey);
  }

  public ParsedUrl(
      boolean iterate,
      String resource,
      List<String> key,
      String placeholderResource,
      List<String> placeholderKey) {
    this(iterate, resource, key, null, placeholderResource, placeholderKey);
  }

  public ParsedUrl(String resource, String technicalId) {
    this(resource, List.of("_id"), technicalId);
  }

  public ParsedUrl(String resource) {
    this.resource = resource;
  }

  private ParsedUrl(
      boolean iterate,
      String resource,
      List<String> key,
      String value,
      String placeholderResource,
      List<String> placeholderKey) {
    this.iterate = iterate;
    this.resource = resource;
    this.key = key;
    this.value = value;
    if (placeholderKey == null || placeholderKey.isEmpty() || placeholderResource == null) {
      this.placeholder = null;
    } else {
      this.placeholder = new DefaultMapEntry<>(placeholderResource, placeholderKey);
    }
  }

  public String getResource() {
    return resource;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public List<String> getKey() {
    return key;
  }

  public void setKey(List<String> key) {
    this.key = key;
  }

  public String getKeyAsString() {
    return StringUtils.join(this.key, ".");
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public DefaultMapEntry<String, List<String>> getPlaceholder() {
    return placeholder;
  }

  public void setPlaceholder(DefaultMapEntry<String, List<String>> placeholder) {
    this.placeholder = placeholder;
  }

  public String toString() {
    String retVal = this.resource;
    if (this.key != null && !this.key.isEmpty()) {
      retVal = retVal + '?' + StringUtils.join(this.key, ".");
    }
    if (this.key != null
        && !this.key.isEmpty()
        && (this.value != null || this.placeholder != null)) {
      retVal += "=";
      if (this.value != null) {
        retVal += value;
      } else {
        String placeholderValue = StringUtils.join(placeholder.getValue(), ".");
        retVal += ("{" + placeholder.getKey() + "." + placeholderValue + "}");
      }
    }
    return retVal;
  }

  public boolean isExecutable() {
    // resource mag nooit leeg zijn
    if (StringUtils.isEmpty(this.resource)) {
      return false;
    }
    // key en value samen mogen leeg zijn
    if ((this.key == null || this.key.isEmpty()) && StringUtils.isEmpty(this.value)) {
      return true;
    }
    // key en value mogen samen opgevuld zijn
    return this.key != null && !this.key.isEmpty() && !StringUtils.isEmpty(this.value);
    // de rest mag niet
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (iterate ? 1231 : 1237);
    result = prime * result + ((key == null) ? 0 : key.hashCode());
    result = prime * result + ((placeholder == null) ? 0 : placeholder.hashCode());
    result = prime * result + ((resource == null) ? 0 : resource.hashCode());
    result = prime * result + ((value == null) ? 0 : value.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ParsedUrl other = (ParsedUrl) obj;
    if (iterate != other.iterate) return false;
    if (key == null) {
      if (other.key != null) return false;
    } else if (!key.equals(other.key)) return false;
    if (placeholder == null) {
      if (other.placeholder != null) return false;
    } else if (!placeholder.equals(other.placeholder)) return false;
    if (resource == null) {
      if (other.resource != null) return false;
    } else if (!resource.equals(other.resource)) return false;
    if (value == null) {
      return other.value == null;
    } else return value.equals(other.value);
  }
}
