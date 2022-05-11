package ca.uhn.fhir.federator;

import java.io.InputStream;
import java.util.Map;

import org.junit.Test;
import org.yaml.snakeyaml.Yaml;

public class TestYaml {
    @Test
    public void testYaml() {
        Yaml yaml = new Yaml();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("federator.yaml");
        Map<String, Object> obj = yaml.load(inputStream);
        System.out.println(obj);
    }

}
