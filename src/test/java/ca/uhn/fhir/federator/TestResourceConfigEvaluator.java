package ca.uhn.fhir.federator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.federator.FederatorProperties.ResourceConfig;
import ca.uhn.fhir.rest.annotation.Read;

public class TestResourceConfigEvaluator {
  @Test
  public void TestTrue() {

    ResourceConfig config = new ResourceConfig();
    config.setRead("true");
    config.setMaxOr(10);
    config.setServer("http://test/fhir");

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, null, config).execute();

    assertEquals(true, actual);
  }

  @Test
  public void TestFalse() {

    ResourceConfig config = new ResourceConfig();
    config.setRead("false");
    config.setMaxOr(10);
    config.setServer("http://test/fhir");

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, null, config).execute();

    assertEquals(false, actual);
  }

  @Test
  public void TestNull() {

    ResourceConfig config = new ResourceConfig();
    config.setRead(null);
    config.setMaxOr(10);
    config.setServer("http://test/fhir");

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, null, config).execute();

    assertEquals(true, actual);
  }

  @Test
  public void TestFhirPathWrong() {

    ResourceConfig config = new ResourceConfig();
    config.setRead("Patient.name=\"Decuypere\"");
    config.setMaxOr(10);
    config.setServer("http://test/fhir");

    Patient patient = new Patient();
    patient.setName(Arrays.asList(new HumanName().setFamily("Decuypere")));

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, patient, config)
            .execute();

    assertEquals(false, actual);
  }

  @Test
  public void TestFhirPathCorrect() {

    ResourceConfig config = new ResourceConfig();
    config.setRead("Patient.name.family=\"Decuypere\"");
    config.setMaxOr(10);
    config.setServer("http://test/fhir");

    Patient patient = new Patient();
    patient.setName(Arrays.asList(new HumanName().setFamily("Decuypere")));

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, patient, config)
            .execute();

    assertEquals(true, actual);
  }

  @Test
  public void TestFhirPathTypeMistake() {

    ResourceConfig config = new ResourceConfig();
    config.setRead("Patient.name.family");
    config.setMaxOr(10);
    config.setServer("http://test/fhir");

    Patient patient = new Patient();
    patient.setName(Arrays.asList(new HumanName().setFamily("Decuypere")));

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, patient, config)
            .execute();

    assertEquals(false, actual);
  }

  @Test
  public void TestFhirPathBogus() {

    ResourceConfig config = new ResourceConfig();
    config.setRead("blablabla");
    config.setMaxOr(10);
    config.setServer("http://test/fhir");

    Patient patient = new Patient();
    patient.setName(Arrays.asList(new HumanName().setFamily("Decuypere")));

    Boolean actual =
        new ResourceConfigEvaluator(FhirContext.forR4Cached(), Read.class, patient, config)
            .execute();

    assertEquals(false, actual);
  }
}
